% taking the current path:
[current_path,name,ext] = fileparts(mfilename('fullpath'));
current_path = [current_path, '\'];

file_name = 'synthetic';

x = load([current_path file_name '.x.txt']);
y = load([current_path file_name '.y.txt']);
z = load([current_path file_name '.z.txt']);

%ylabel('y','FontSize',fontsize);
%xlabel('x','FontSize',fontsize);
%zlabel('P(...)','FontSize',fontsize);
%title(fig_title);

figure(2);

contourf(x,y,z);

eps_file = [current_path file_name '-contour.eps'];
print('-depsc', eps_file);
disp(eps_file);
system(['epstopdf ', eps_file]);

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

%ylabel('y_1','FontSize',fontsize);
%xlabel('x_1','FontSize',fontsize);
%zlabel('P(...)','FontSize',fontsize);
%title(fig_title);
figure(1);
%print(gcf, '-depsc', ['radar.',id,'.eps']);

eps_file = [current_path file_name '.eps'];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
disp(eps_file);

system(['epstopdf ', eps_file]);
