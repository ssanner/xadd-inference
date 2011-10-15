[x,y]=meshgrid(1:15,1:15);
z = peaks(15);
x = x(:); y = y(:); z = z(:);
tri = delaunay(x,y);
trisurf(tri,x,y,z)
