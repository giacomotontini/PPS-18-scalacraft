checkUp(X,Y,Z,Result):- state(X, Y, Z, "surface"), state(X,Y1,Z, "noSurface"), Y1 is Y+1, Result=pos(X,Y1,Z).
checkDown(X,Y,Z,Result):- state(X,Y1,Z, "noSurface"), Y1 is Y+1, state(X,Y,Z, "noSurface"), state(X,Y2,Z, "noSurface"), Y2 is Y-1, state(X,Y3,Z, "surface"), Y3 is Y-2, Result=pos(X,Y2,Z).
checkSameLevel(X,Y,Z,Result):- state(X,Y,Z,"noSurface"), state(X,Y1,Z, "surface"), Y1 is Y-1, Result=pos(X,Y,Z).

move(X,Y,Z,Result):-
    checkUp(X,Y,Z,Result);
    checkDown(X,Y,Z,Result);
    checkSameLevel(X,Y,Z,Result).

moves(X,Y,Z,Result) :-
    move(X1,Y,Z,Result), X1 is X+1;
    move(X2,Y,Z,Result), X2 is X-1;
    move(X,Y,Z1,Result), Z1 is Z+1;
    move(X,Y,Z2,Result), Z2 is Z-1.