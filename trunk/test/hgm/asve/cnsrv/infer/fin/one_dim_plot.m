function one_dim_plot(complete_file_name, fig_title)

font_size = 14;

data = load([complete_file_name, '.txt']);

vis = 'on';
figure('visible', vis);
hold on;
plot(data(:,1),data(:,2), '-k', 'LineWidth', 1);
hold off;
xlabel('X', 'FontSize', font_size);
ylabel('Y', 'FontSize', font_size);
title(fig_title);

%generate pdf:
print('-depsc', [complete_file_name '.eps']);
disp(complete_file_name);

system(['epstopdf ' complete_file_name '.eps']);


