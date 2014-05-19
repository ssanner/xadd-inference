% plot3d('1')
function [] = plot3d(id) %, subfig)

fontsize = 14;

str = ['radar.query.',id,'.x.txt'];
eval(['load ',str]);
x = radar;

str = ['radar.query.',id,'.y.txt'];
eval(['load ',str]);
y = radar;

str = ['radar.query.',id,'.z.txt'];
eval(['load ',str]);
z = radar;

figure(2);
contourf(x,y,z);

%figure(1);
%plot3(x,y,z);

figure(1);
surf(x,y,z);
%shading faceted
%shading interp;
%shading phong
%colormap jet
rotate3d;

%figure(3);
%mesh(x,y,z);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

ylabel('y_1','FontSize',fontsize);
xlabel('x_1','FontSize',fontsize);
zlabel('P(...)','FontSize',fontsize);

figure(1);
print(gcf, '-depsc', ['radar.',id,'.eps']);


