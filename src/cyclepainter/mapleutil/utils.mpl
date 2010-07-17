# Copyright 2009 Maplesoft, Tim Northover

# Who knows what license the butchered Maple functions are under, they
# were obtained from showstat. The entire thing needs maple, which
# would have a copy anyway so I see no harm.

# My bits are GPL3.

argument_mean := proc(theta1, theta2)
local poss, inPt, possPt;
	poss := (theta1+theta2)/2;
	inPt := evalf(exp(I*theta1));
	possPt := evalf(exp(I*poss));

	if abs(possPt - inPt) < abs(possPt + inPt) then
		return poss
	else
		poss := evalf(poss + Pi);
		if poss > evalf(Pi) then
			return evalf(poss-2*Pi);
		else
			return poss;
		fi;
	fi
end proc:

angle_between := proc(t1, t2)
    local poss;
    poss := t2 - t1;
    if poss < 0 then poss := evalf(poss + 2*Pi) fi;
    if poss > evalf(Pi) then poss := evalf(2*Pi - poss) fi;
    return poss;
end proc:

lift := proc(curve, startSheets, start, fin)
local res, indexed, finSheets, i, best;
	res := `algcurves/Acontinuation`(curve:-f, curve:-x, curve:-y, start*(1-t) + fin*t, t, 7):

	indexed := ListTools:-Enumerate(res[1][2]);

	finSheets := [0 $ nops(startSheets)];
	for i from 1 to nops(startSheets) do
		best := sort(indexed, (x,y) -> abs(x[2] - startSheets[i]) < abs(y[2] - startSheets[i]))[1];
		finSheets[i] := res[-1][2][best[1]];
	od;
	return finSheets;
end proc:

central_monodromy := proc(curve, base)
local finiteBs, wheel, argComparison, spokeAngle1, spokeAngle2, theta, i, j, radius, sheets, sheetsEnd, best, bestDist, perm, liftTriangle, monodromy, branchPerm, branch, curPerm;
	finiteBs := [fsolve(discrim(curve:-f, curve:-y), curve:-x, complex)];
	finiteBs := ListTools:-MakeUnique(finiteBs);

	# Only really care relative to base
	wheel := map(x -> x-base, finiteBs);

	radius := 1.2*max(seq(abs(x), x in wheel));

	# Sort the list by argument from base, keeping track of what
	# the actual permutation was
	wheel := map(argument, wheel);
	wheel := ListTools:-Enumerate(wheel);
	wheel := sort(wheel, (x,y) -> x[2] < y[2]);
	perm, wheel := op(ListTools:-Transpose(wheel));

	sheets := [fsolve(subs(curve:-x = base, curve:-f), curve:-y, complex)];
	liftTriangle := proc(theta1, theta2)
	local pt1, pt2, tmp;
		pt1 := base + radius*exp(I*theta1);
		pt2 := base + radius*exp(I*theta2);
		tmp := lift(curve, sheets, base, pt1);
		tmp := lift(curve, tmp, pt1, pt2);
		tmp := lift(curve, tmp, pt2, base);
        return tmp;
	end proc;

	spokeAngle1 := argument_mean(wheel[1], wheel[-1]);
	wheel := [op(wheel), wheel[1]];

	for branch from 1 to nops(wheel)-1 do
		spokeAngle2 := argument_mean(wheel[branch], wheel[branch+1]);

        # Modify variables so the triangle always has central angle <
        # pi/6ish
        if angle_between(spokeAngle1, spokeAngle2) > 0.5 then
            if angle_between(spokeAngle1, wheel[branch]) >= 0.25 and angle_between(spokeAngle2, wheel[branch]) >= 0.25 then
                # Both are fine, narrow the entire field to pi/6ish
                spokeAngle1 := wheel[branch] - 0.25;
                spokeAngle2 := wheel[branch] + 0.25;
            elif angle_between(spokeAngle1, wheel[branch]) < angle_between(spokeAngle2, wheel[branch]) then
                # spokeAngle2 needs changing
                spokeAngle2 := spokeAngle1 + 0.5;
            else
                spokeAngle1 := spokeAngle2 - 0.5;
            fi;
        fi;

		sheetsEnd := liftTriangle(spokeAngle1, spokeAngle2);

        branchPerm := [0 $ nops(sheets)];
		for i from 1 to nops(sheets) do
			bestDist := infinity;

			for j from 1 to nops(sheets) do
				if abs(sheets[i]-sheetsEnd[j]) < bestDist then
					best := j;
					bestDist := abs(sheets[i]-sheetsEnd[j]);
				fi;
			od;
			branchPerm[i] := best;
		od;

		monodromy[branch] := [finiteBs[perm[branch]], group[invperm](convert(branchPerm, `disjcyc`))];

		spokeAngle1 := spokeAngle2;
	od;

	monodromy := convert(monodromy, list);

	# Finally add in infinity if necessary
	curPerm := [];
	for branch in monodromy do
		curPerm := group[mulperms](curPerm, branch[2]);
	od;

	if curPerm <> [] then
		monodromy := [op(monodromy), [infinity, group[invperm](curPerm)]];
	fi;

	return [base, sheets, monodromy];
end proc:

displace_sheet_base := proc(curve, y0, dest)
local solns;
    solns := fsolve(subs(curve:-x=dest, curve:-f), curve:-y, 'complex');
    solns := sort([solns], (a,b) -> abs(a-y0) < abs(b-y0));

    return solns[1];
end proc:

best_match := proc(x, l)
local tmp;
    tmp := ListTools[Enumerate](l);
    tmp := sort(tmp, (a,b) -> abs(a[2]-x) < abs(b[2]-x));
    return tmp[1];
end proc:

# Computes the image of x under the permutation p expressed in
# disjoint cycle notation.
apply_perm := proc(p, x)
  local cycle, pos;
  for cycle in p do
    if member(x, cycle, 'pos') then
       if pos = nops(cycle) then
           return cycle[1];
       else
           return cycle[pos+1];
       fi;
    fi;
  od;
  return x;
end proc:

# Applies permutation to a list of elements.
permute_list := proc(l, perm)
local cycle, res, i, backup, nxt;
    res := Array(l);
    for cycle in perm do
        nxt := res[cycle[1]];
        for i from 2 to nops(cycle) do
            backup := res[cycle[i]];
            res[cycle[i]] := nxt;
            nxt := backup;
        od;
        res[cycle[1]] := nxt;
    od;

    return convert(res, list);
end proc:

# Helper for mono_to_shift. Calculates the angle between vec-centre
# and pt-centre. Return value is +ve so sorting orders rays from
# centre starting at vec.
arg_from_pt := proc(pt, centre, vec)
local res;
    if pt = infinity then
        res := evalf(argument(-conjugate(vec-centre)));
    else
        res := evalf(argument((pt-centre)*conjugate(vec-centre)));
    fi;
    if res < 0 then return evalf(res+2*Pi) else return res fi;
end proc:

# Converts the output of monodromy into a description of the effects
# of crossing a branch cut.
mono_to_shift := proc(monodromy, base, sheets_base)
local sorted, mono, modification, shift, cuts, curPerm;
    sorted := sort(monodromy, (a,b) -> arg_from_pt(a[1], base, sheets_base) < arg_from_pt(b[1], base, sheets_base));
    modification := [];
    cuts := [];

    for mono in sorted do
        curPerm := group[invperm](mono[2]);

        # l_i = c_i . m_b_i ^ -1 . c_i^-1
        shift := group[mulperms](group[invperm](modification), curPerm);
        shift := group[mulperms](shift, modification);

        cuts := [op(cuts), [mono[1], shift]];

        # c_{i+1} = c_i . m_b_i ^ -1
        modification := group[mulperms](curPerm, modification);
    od;
    return cuts;
end proc:

lift_point := proc(curve, src, dst, init_sheets)
local vals, final_sheets, sheet, sorted_sheets, done_limit;
global time_limit;

    if time_limit = 'time_limit' then
        time_limit := kernelopts(cputime)+2.0;
        done_limit := true;
    fi;

    vals := limited_Acontinuation(curve:-f, curve:-x, curve:-y, src+t*(dst-src), t, 10);
    vals := zip((x,y) -> [x,y], vals[1][2], vals[-1][2]);

    final_sheets := [];
    for sheet in init_sheets do
        sorted_sheets := sort(vals, (a,b) -> abs(a[1]-sheet) < abs(b[1]-sheet));
        final_sheets := [op(final_sheets), sorted_sheets[1][2]];
    od;

    if done_limit = true then
        time_limit := 'time_limit';
    fi;

    return final_sheets;
end proc:

`algcurves/Lmonodromy` := proc(curve, x, y)
local xvalue, preimages;
global use_base;
   xvalue := 0;
   if use_base <> 'use_base' then
       xvalue := use_base;
   else
       while subs(x = xvalue,lcoeff(curve,y)) = 0 do
           xvalue := xvalue-1
       end do;
   fi;
   preimages := [`algcurves/fsolve`(subs(x = xvalue,curve),y,complex)];
   [xvalue, preimages, []]
end proc:

`algcurves/Gmonodromy` := proc(curve, x, y)
local discpoints, n, preimages, xvalue, paths, r, i, j, k, t, orderedpt, permpoint, permutations, cyc_struct, bpoints, dig, Pp, Q, rootof;
global `algcurves/A1`, use_base;
   dig := Digits;
   Digits := dig+2;
   n := degree(curve,y);
   discpoints := NULL;
   for i in evala(Factors(lcoeff(curve,y)^2*discrim(curve,y),indets(curve,{radical, algext})))[2] do
     if 1 < i[2] then
       r := algcurves['puiseux'](curve,x = RootOf(i[1],x),y,`cycle structure`)
     else
       r := [`$`(1,n-2), 2]
     end if;
     k := `algcurves/fsolve`(i[1],x,complex);
     if `algcurves/realcurve`(curve,x,y) and {k} <> map(conjugate,{k}) then
       k := seq(op(`if`(0 < Im(j),[],[j, conjugate(j)])),j = k)
     end if;
     discpoints := discpoints, k;
     for j in [k] do
       rootof[j] := RootOf(i[1],x);
       cyc_struct[j] := r
     end do
   end do;
   discpoints := `algcurves/Sortnumbers`([discpoints]);
   r := table();
   for j in discpoints do
     r[j] := min(seq(.4000000000*abs(i-j),i = `minus`({op(discpoints)},{j})))
   end do;
   if nops(discpoints) = 1 then
     r[discpoints[1]] := 1
   end if;
   if Im(discpoints[1]) <> 0 and `algcurves/realcurve`(curve,x,y) then
     discpoints := [evalf(Re(discpoints[1]-r[discpoints[1]])-1/10), op(discpoints)];
     r[discpoints[1]] := -1/1000;
     cyc_struct[discpoints[1]] := [`$`(1,n)]
   end if;

   if use_base <> 'use_base' then
       xvalue := use_base;
   else
       xvalue := discpoints[1]-r[discpoints[1]];
   fi;
   preimages := [`algcurves/fsolve`(subs(x = xvalue,curve),y,'complex')];
   paths := `algcurves/Makepaths`(t,discpoints,xvalue,r);
   if member('showpaths',`algcurves/A1`) then
     print(plot(`algcurves/Showpaths`(paths,r,t),('labels') = ['Re', 'Im'],('scaling') = ('CONSTRAINED'),('color') = ('red'),('title') = cat("Paths chosen for the analytic continuation for ",convert(y(x),'string')),('axes') = ('NORMAL')))
   end if;
   userinfo(1,'algcurves',`Starting the analytic continuation...`);
   permutations := table();
   bpoints := `algcurves/Argsort`([seq(`if`(cyc_struct[i] = [`$`(1,n)],NULL,i),i = discpoints)],xvalue);
   k := 0;
   Digits := dig;
   for i in bpoints do
     k := k+1;
     if n = 2 then
       permutations[i] := [2, 1]
     elif 0 < Im(i) and `algcurves/realcurve`(curve,x,y)
     	  and use_base = 'use_base' then
       if not assigned(Pp) then
         Pp := `algcurves/Permuted`(preimages,map(conjugate,preimages))
       end if;
       Q := `algcurves/Permuted`(permutations[conjugate(i)],[seq(j,j = 1 .. n)]);
       permutations[i] := [seq(Pp[Q[Pp[j]]],j = 1 .. n)]
     else
       orderedpt := preimages;
       for j in paths[i] do
         orderedpt := `algcurves/Propagate`(limited_Acontinuation(curve,x,y,j,t,Digits),orderedpt)
       end do;
       permpoint := `algcurves/Propagate`(limited_Acontinuation(curve,x,y,i-r[i]*exp(I*Pi*t),t,Digits),orderedpt);
       permpoint := `algcurves/Propagate`(limited_Acontinuation(curve,x,y,i+r[i]*exp(I*Pi*t),t,Digits),permpoint);
       permutations[i] := `algcurves/Permuted`(orderedpt,permpoint)
     end if;
     `algcurves/check_cstruct`(permutations[i],cyc_struct[i], i);
     userinfo(2,'algcurves','`Computed the permutation around`',k,'of',nops(bpoints),'`branch points.`')
   end do;
   [xvalue, preimages, [seq([i, permutations[i]],i = bpoints)], discpoints, paths, r, rootof]
end proc:

`algcurves/Makepaths` := proc(t, pnts, x0, rOrig)
local paths, i, j, used, connecting, p, q, outpt, pts, inpt, ptj, ptjp1, ptjm1, linepaths, points, pointseq, m, k, rampoints, n, tau, circlepaths, connectionpt, newpath, rr, jj, pj, notseq, tau1, tau2, p1, p2, mm, nn, omega1, omega2, r;
   points := [x0, op(pnts)];
   r := rOrig;
   r[x0] := 0;

   used := [points[1]];
   pointseq := table();
   pointseq[points[1]] := [points[1]];
   connecting := table();
   points := `algcurves/Distsort`(points,x0);
   for k from 2 to nops(points) do
     q := points[k];
     pointseq[q] := [points[1]];
     for i in [seq(used[j],j = 2 .. nops(used))] do
       p := op(nops(pointseq[q]),pointseq[q]);
       m := p-r[p];
       if abs(p+r[p]-q) < abs(m-q) then
         m := p+r[p]
       end if;
       n := q-r[q];
       if abs(q+r[q]-m) < abs(n-m) then
         n := q+r[q]
       end if;
       tau := Re(evalc(conjugate(i-m))*(n-m))/abs(n-m)^2;
       if abs(Im(evalc(conjugate(i-m))*(n-m))/(n-m)) < abs(r[i]) and 0 <= tau and tau <= 1 then
         pointseq[q] := pointseq[i]
       end if
     end do;
     p := op(nops(pointseq[q]),pointseq[q]);
     m := p-r[p];
     if abs(p+r[p]-q) < abs(m-q) then
       m := p+r[p]
     end if;
     n := q-r[q];
     if abs(q+r[q]-m) < abs(n-m) then
       n := q+r[q]
     end if;
     connecting[p,q] := [m, n];
     pointseq[q] := [op(pointseq[q]), q];
     used := [op(used), q];
     notseq := [op(`minus`({op(points)},{op(pointseq[q])}))];
     i := 1;
     while i <= nops(pointseq[q])-1 do
       m, n := op(connecting[pointseq[q][i],pointseq[q][i+1]]);
       mm := x0+(q-x0)*Re(evalc(conjugate(m-x0))*(q-x0))/abs(q-x0)^2;
       nn := x0+(q-x0)*Re(evalc(conjugate(n-x0))*(q-x0))/abs(q-x0)^2;
       j := 1;
       while 0 < j and j <= nops(notseq) do
         pj := notseq[j];
         tau1 := Re(evalc(conjugate(pj-m))*(n-m))/abs(n-m)^2;
         tau2 := Re(evalc(conjugate(pj-mm))*(nn-mm))/abs(nn-mm)^2;
         if Re(n-m) <> 0 then
           omega1 := Im(n-m)/Re(n-m);
           p1 := Im(pj)-omega1*Re(pj)-Im(m)+omega1*Re(m);
           omega2 := Im(q-x0)/Re(q-x0);
           p2 := Im(pj)-omega2*Re(pj)-Im(x0)+omega2*Re(x0)
         else
           omega1 := 0;
           p1 := Re(pj-m);
           omega2 := Re(q-x0)/Im(q-x0);
           p2 := Re(pj)-omega2*Im(pj)-Re(x0)+omega2*Im(x0)
         end if;
         if 0 <= omega1*omega2 and p1*p2 < 0 and 0 <= tau1 and tau1 <= 1 and 0 <= tau2 and tau2 <= 1 or omega1*omega2 < 0 and 0 < p1*p2 and 0 <= tau1 and tau1 <= 1 and 0 <= tau2 and tau2 <= 1 then
           pointseq[q] := [seq(pointseq[q][jj],jj = 1 .. i), pj, seq(pointseq[q][jj],jj = i+1 .. nops(pointseq[q]))];
           m := pointseq[q][i]-r[pointseq[q][i]];
           if abs(pointseq[q][i]+r[pointseq[q][i]]-pj) < abs(m-pj) then
             m := pointseq[q][i]+r[pointseq[q][i]]
           end if;
           n := pj-r[pj];
           if abs(pj+r[pj]-m) < abs(n-m) then
             n := pj+r[pj]
           end if;
           connecting[pointseq[q][i],pj] := [m, n];
           m := pj-r[pj];
           if abs(pj+r[pj]-pointseq[q][i+2]) < abs(m-pointseq[q][i+2]) then
             m := pj+r[pj]
           end if;
           n := pointseq[q][i+2]-r[pointseq[q][i+2]];
           if abs(pointseq[q][i+2]+r[pointseq[q][i+2]]-m) < abs(n-m) then
             n := pointseq[q][i+2]+r[pointseq[q][i+2]]
           end if;
           connecting[pj,pointseq[q][i+2]] := [m, n];
           notseq := [op(`minus`({op(notseq)},{pj}))];
           j := 0
         else
           j := j+1
         end if
       end do;
       if nops(notseq) < j then
         i := i+1
       end if
     end do
   end do;

   points := select(i -> i <> x0, points);
   rampoints := `algcurves/Argsort`(points,x0);

   paths := table();
   paths['parameter'] := t;
   for i in rampoints do
     if 1 < nops(pointseq[i]) then
       linepaths := [];
       for j from 2 to nops(pointseq[i]) do
         pts := connecting[pointseq[i][j-1],pointseq[i][j]];
         linepaths := [op(linepaths), (1-t)*pts[1]+t*pts[2]]
       end do;
       pts := [subs(t = 0,linepaths[1]), subs(t = 1,linepaths[1])];
       rr := r[pointseq[i][1]];
       if abs(pts[1]-pointseq[i][1]+rr) < 10^(-Digits+2) then
         if Im(pts[2]-pts[1]) < 0 and 0 <= Im(i-pointseq[i][1]) then
           circlepaths := [[pointseq[i][1]-rr*exp(-I*Pi*t), pointseq[i][1]+rr*exp(-I*Pi*t)]]
         elif 0 < Im(pts[2]-pts[1]) and Im(i-pointseq[i][1]) < 0 then
           circlepaths := [[pointseq[i][1]-rr*exp(I*Pi*t), pointseq[i][1]+rr*exp(I*Pi*t)]]
         else
           circlepaths := [[]]
         end if
       else
         if 0 <= Im(i-pointseq[i][1]) then
           circlepaths := [[pointseq[i][1]-rr*exp(-I*Pi*t)]]
         else
           circlepaths := [[pointseq[i][1]-rr*exp(I*Pi*t)]]
         end if
       end if;
       for j from 2 to nops(pointseq[i])-1 do
         newpath := [];
         ptj := pointseq[i][j];
         ptjm1 := pointseq[i][j-1];
         ptjp1 := pointseq[i][j+1];
         inpt := connecting[ptjm1,ptj][2];
         outpt := connecting[ptj,ptjp1][1];
         if Im(i-x0)/Re(i-x0) < Im(ptj-x0)/Re(ptj-x0) then
           if inpt = ptj-r[ptj] and outpt = ptj+r[ptj] then
             newpath := [ptj-r[ptj]*exp(I*Pi*t)]
           elif inpt = ptj+r[ptj] and outpt = ptj-r[ptj] then
             newpath := [ptj+r[ptj]*exp(I*Pi*t)]
           elif inpt = ptj-r[ptj] and outpt = inpt and 0 < Im(ptjp1-ptjm1) then
             newpath := [ptj-r[ptj]*exp(I*Pi*t), ptj+r[ptj]*exp(I*Pi*t)]
           elif inpt = ptj+r[ptj] and outpt = inpt and Im(ptjp1-ptjm1) < 0 then
             newpath := [ptj+r[ptj]*exp(I*Pi*t), ptj-r[ptj]*exp(I*Pi*t)]
           end if
         else
           if inpt = ptj-r[ptj] and outpt = ptj+r[ptj] then
             newpath := [ptj-r[ptj]*exp(-I*Pi*t)]
           elif inpt = ptj+r[ptj] and outpt = ptj-r[ptj] then
             newpath := [ptj+r[ptj]*exp(-I*Pi*t)]
           elif inpt = ptj-r[ptj] and outpt = inpt and Im(ptjp1-ptjm1) < 0 then
             newpath := [ptj-r[ptj]*exp(-I*Pi*t), ptj+r[ptj]*exp(-I*Pi*t)]
           elif inpt = ptj+r[ptj] and outpt = inpt and 0 < Im(ptjp1-ptjm1) then
             newpath := [ptj+r[ptj]*exp(-I*Pi*t), ptj-r[ptj]*exp(-I*Pi*t)]
           end if
         end if;
         circlepaths := [op(circlepaths), newpath]
       end do;
       connectionpt := connecting[pointseq[i][-2],i][2];
       if abs(connectionpt-i+r[i]) < 10^(-Digits+2) then
         circlepaths := [op(circlepaths), []]
       elif 0 <= Im(i-x0) then
         newpath := [i+r[i]*exp(-I*Pi*t)];
         circlepaths := [op(circlepaths), newpath]
       else
         newpath := [i+r[i]*exp(I*Pi*t)];
         circlepaths := [op(circlepaths), newpath]
       end if;
       paths[i] := op(circlepaths[1]);
       for j from 2 to nops(pointseq[i]) do
         paths[i] := paths[i], linepaths[j-1];
         if circlepaths[j] <> [] then
           paths[i] := paths[i], op(circlepaths[j])
         end if
       end do;
       paths[i] := [paths[i]]
     else
       paths[i] := []
     end if
   end do;
   eval(paths)
end proc:

`algcurves/Argsort` := proc(ll, b)
local dec;
   dec := (s, t) -> evalb(s = b or Im(s-b)*Re(t-b) < Im(t-b)*Re(s-b) or Im(s-b)*Re(t-b) = Im(t-b)*Re(s-b) and Im(s-b)^2+Re(s-b)^2 < Im(t-b)^2+Re(t-b)^2);
   dec := (s, t) -> argument(s-b) < argument(t-b);
   sort(ll,dec)
end proc:


limited_Acontinuation := proc(curve, x, y, path, t, DI)
local tstep, i, fibre, t1, dig, t_old, R, CL;
global `algcurves/v_close`, time_limit;
    i := exp(-I*Pi*t);
    if has(path,i) then
        t1 := procname(curve,x,y,coeff(path,i,0)-coeff(path,i,1)*exp(I*Pi*t),t,DI);
        [seq([1-t1[-i][1], t1[-i][2]],i = 1 .. nops(t1))]
    else
        userinfo(6,'algcurves',`following path`,path);
        fibre := [`algcurves/fsolve`(subs(x = subs(t = 0,path),curve),y,complex)];
        R := [0, fibre];
        dig := Digits;
        CL := round(sqrt(max(1,dig-8)));
        if assigned(_Env_algcurves_CL) then
            CL := _Env_algcurves_CL
        end if;
        tstep := 1/8/CL^2;
        t_old := 0;
        while t_old < 1 do
            if time_limit <> 'time_limit' and
                kernelopts(cputime) > time_limit then
                error "Time limit exceeded in analytic continuation";
            end if;

            t1 := t_old+tstep;
            userinfo(9,'algcurves',`Digits, tstep, t1 `,Digits,tstep,t1);
            fibre := `algcurves/Continue`(CL,curve,x,y,path,t,t_old,t1,fibre);
            R := R, fibre;
            fibre := [fibre][-1][2];
            if `algcurves/v_close` = 1 and tstep < 1/(4+CL) then
                tstep := 2*tstep;
                if dig < Digits then
                   Digits := Digits-3
                end if
           elif `algcurves/v_close` = -1 then
               tstep := 1/2*tstep;
               if tstep < 1/16/CL^2 then
                   Digits := Digits+3
               end if
           end if;
           tstep := min(tstep,1-t1);
           t_old := t1
        end do;
        [R]
    end if
end proc:
