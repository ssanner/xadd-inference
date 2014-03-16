% taking the current path:
[current_path,name,ext] = fileparts(mfilename('fullpath'));
current_path = [current_path, '\'];

% parsing info file:
data = load([current_path, 'db_Train10_Test600_Dim2_500item_56079_samples']);

vis = 'on';
figure('visible', vis);
hold on;
%plot(data(:,1),data(:,2), '-k', 'LineWidth', 1);
scatter(data(:,1),data(:,2), 3)
hold off;

%%figure('visible', vis);
%%hist(data)   %HISTOGRAM...

%xlabel('X', 'FontSize', font_size);
%ylabel('Y', 'FontSize', font_size);
%title(fig_title);

%generate pdf:

eps_file = [current_path 'scatter2D.eps'];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
disp(eps_file);

system(['epstopdf ', eps_file]);


%print('-depsc', [current_path 'scatter2D.eps']);
%disp([current_path 'scatter2D']);
%system(['epstopdf ' current_path 'scatter2D.eps']);

%................................

%[data_files, titles, dims] = textread([current_path, 'scatter2D.txt'], '%s %s %f');

%for i=1:length(data_files)
  %if dims(i)== 1
      %one_dim_scatter_plot([current_path, data_files{i}], titles{i});
 % end %if

 % if dims(i) == 2
 %     two_dim_plot([current_path, data_files{i}], titles{i});
 % end %if
%end %for

