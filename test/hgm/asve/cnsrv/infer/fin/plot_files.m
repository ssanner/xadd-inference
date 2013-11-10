
% taking the current path:
[current_path,name,ext] = fileparts(mfilename('fullpath'));
current_path = [current_path, '\'];

% parsing info file:
[data_files, titles, dims] = textread([current_path, 'info.txt'], '%s %s %f');

for i=1:length(data_files)
  if dims(i)== 1      
      one_dim_plot([current_path, data_files{i}], titles{i});
  end %if
  
  if dims(i) == 2
      two_dim_plot([current_path, data_files{i}], titles{i});
  end %if
end %for

%two_dim_plot([current_path, 'radar.query.3']);
%two_dim_plot([current_path, 'radar.query.4']);

%one_dim_plot([current_path, 'radar.query.1']);
%one_dim_plot([current_path, 'tracking.query.1']);
%one_dim_plot([current_path, 'tracking.query.2']);
%one_dim_plot([current_path, 'tracking.query.3']);
%one_dim_plot([current_path, 'tracking.query.4']);
%one_dim_plot([current_path, 'tracking.query.5']);
%one_dim_plot([current_path, 'tracking.query.6']);

