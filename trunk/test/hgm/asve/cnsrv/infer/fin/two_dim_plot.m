function [] = two_dim_plot(file_path, fig_title) 

fontsize = 14;

%x = load(['E:\WORK\matlab\data2\radar.query.',id,'.x.txt']);
x = load([file_path, '.x.txt']);

%y = load(['E:\WORK\matlab\data2\radar.query.',id,'.y.txt']);
y = load([file_path, '.y.txt']);

%z = load(['E:\WORK\matlab\data2\radar.query.',id,'.z.txt']);
z = load([file_path, '.z.txt']);

ylabel('y_1','FontSize',fontsize);
xlabel('x_1','FontSize',fontsize);
zlabel('P(...)','FontSize',fontsize);
title(fig_title);

figure(2);

contourf(x,y,z);

eps_file = [file_path, '-contour.eps'];
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

ylabel('y_1','FontSize',fontsize);
xlabel('x_1','FontSize',fontsize);
zlabel('P(...)','FontSize',fontsize);
title(fig_title);
figure(1);
%print(gcf, '-depsc', ['radar.',id,'.eps']);

eps_file = [file_path, '.eps'];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
disp(eps_file);

system(['epstopdf ', eps_file]);
