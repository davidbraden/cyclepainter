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
