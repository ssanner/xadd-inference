
% taking the current path:
[current_path,name,ext] = fileparts(mfilename('fullpath'));
current_path = [current_path, '\'];

% parsing info file:
data = load([current_path, 'scatter2D_anglican10000.txt']);

vis = 'on';
figure('visible', vis);
hold on;
%plot(data(:,1),data(:,2), '-k', 'LineWidth', 1);
scatter(data(:,1),data(:,2), 3);
xlim([1.2 2.1]);
ylim([1.2 2]);

font_size = 10;
xlabel('M_1', 'FontSize', font_size);
ylabel('V_1', 'FontSize', font_size);

%other_data = load([current_path, 'other_scatter2D.txt']);
%scatter(other_data(:,1),other_data(:,2), 'red', 'x');
%legend({'Symbolic Gibbs', 'Anglican'}, 'Location','southeast');

hold off;


%figure('Name', 'data 1');
%hold on;
%hist(data(:,1));
%hold off;

%figure('Name', 'data 2');
%hold on;
%hist(data(:,2));
%hold off;


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

