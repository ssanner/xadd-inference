function artplot( data, alg_names, colors, x_label, y_label, scaling_type, plot_type, current_path, file_title )
%NOTE at least data{1} should not be EMPTY
columns = size(data{1}, 2);
index = -1;
if columns == 3
    index = 0;
elseif columns == 4
    index = 1;
else
    error('I think the data is not found. Check parameters...')
end

lines = {':' , ':', '--', '--', '.', '.', '-', '-', '-', '-', '-', '-'};

numAlgs = size(data, 2);

%figureHandle = gcf;
%# make all text in the figure to size 14 and bold
%set(findall(figureHandle,'type','text'),'fontSize',14)

vis = 'on';
figure('visible', vis);

%just to create semi log axis:
if strcmp(scaling_type,'loglog')
loglog(data{1}(:,1 + index),data{1}(:,2 + index),[colors{1} '-'], ...
    'LineWidth',3);
end
if strcmp(scaling_type,'semilogx')
semilogx(data{1}(:,1 + index),data{1}(:,2 + index),[colors{1} '-'], ...
    'LineWidth',3);
end
if strcmp(scaling_type,'semilogy')
semilogy(data{1}(:,1 + index),data{1}(:,2 + index),[colors{1} '-'], ...
    'LineWidth',3);
end
if strcmp(scaling_type,'plot')
plot(data{1}(:,1 + index),data{1}(:,2 + index),[colors{1} '-'], ...
    'LineWidth',3);
end


hold on;

for i=2:numAlgs
    c = [colors{i} lines{i}];
    if size(data{i}, 2) < 3
        data{i} = [0, 0, 0, 0; 0, 0, 0, 0];
    end
    plot(data{i}(:,1 + index),data{i}(:,2 + index),c, ...
        'LineWidth',3);
end

leg = legend(alg_names, 'location', 'southeast');%(algs);
xlabel(x_label);
ylabel(y_label)

%transparent = true;
%for i=1:numAlgs
%    c = [colors{i} ':'];
%    A = transpose(info1{i}(:,1));
%    B = transpose(info1{i}(:,2));
%    C = transpose(info1{i}(:,3));
%    if transparent
        %now error bounds (UP):   
%        semilogx(A, B + C,c);
        %now error bounds (DOWN):   
%        semilogx(A, max (B - C, 0), c);
%    else
    %if C is negative the logarithmic diagram becomes crazy....
%    C0 = 0.999*(B - max(B - C, 0));
%    shadedErrorBar(A,B,C0,c);
%    end %if transparent
%end %for

if strcmp(plot_type, 'shaded') 
    if ~strcmp(scaling_type, 'plot') 
        %error('only works with plot');
    end 
for i=1:numAlgs    
    %if C is negative the logarithmic diagram becomes crazy....
    c = [colors{i} ':'];
    A = transpose(data{i}(:,1 + index));
    B = transpose(data{i}(:,2 + index));
    C = transpose(data{i}(:,3 + index));
    
    C0 = 0.999*(B - max(B - C, 0));
    shadedErrorBar(A,B,C0*2.05,c, true);
end
end

if strcmp(plot_type, 'halo')
for i=1:numAlgs
    c = [colors{i} ':'];
    A = transpose(data{i}(:,1 + index));
    B = transpose(data{i}(:,2 + index));
    C = transpose(data{i}(:,3 + index));
        %now error bounds (UP):   
        plot(A, B + C,c);
        %now error bounds (DOWN):   
        plot(A, max (B - C, 0), c);        
end %for
end 

if strcmp(plot_type, 'errorbar')
for i=1:numAlgs
    c = [colors{i} lines{i}];
    errorbar(data{i}(:,1 + index), data{i}(:,2 + index), data{i}(:,3 + index)*2.05, c);
end
end

figureHandle = gcf;
%# make all text in the figure to size 14 and bold
font_size = 15;
set(findall(figureHandle,'type','text'),'fontSize',font_size)
set(gca,'fontSize',font_size)

hold off;

%%%%%%%%%%%%%%%%%%
%generate pdf:
complete_file_name = strcat(file_title, '-', plot_type, '.eps' );
eps_file = [current_path complete_file_name];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
%disp(eps_file);
system(['epstopdf ', eps_file]);
end

