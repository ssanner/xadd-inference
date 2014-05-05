% taking the current path:
%[current_path,name,ext] = fileparts(mfilename('fullpath'));
%current_path = [current_path, '\'];
current_path = 'E:\REPORT_PATH\';

% parsing info file:


vis = 'on';
figure('visible', vis);
%hold on;

x = 0:1000;
y = log(x);

semilogx(x,y)

samples_fixed = 1;
time_fixed = 2;

%%%%%%%%%
analysisType = time_fixed; %time_fixed; %samples_fixed; %time_fixed;
dims = 6;
cnstrs = 6;
samples = 10000;
time_if_fixed = 30000;
numQ = 20;
%algs = {'rej', 'metro', 'gated', 'full', 'symbolic'};
algs = {'rej.bayes.mmm', 'gated.bayes.mmm', 'mh.bayes.mmm', 'full.gibbs.bayes.mmm'}; %{'gated', 'gated.bayes'};
%algs = {'rej.bayes.bppl', 'gated.const.bppl', 'mh.bayes.bppl', 'full.gibbs.bayes.bppl'}; % , 'full.gibbs.bayes.mmm'}; %{'gated', 'gated.bayes'};
colors = {'k', 'b', 'r', 'g', 'c'};
numAlgs = size(algs, 2);
%%%%%%%%%

%%%%%%%%%
%dims = 6;
%cnstrs = 9;
%samples = 10000;
%numQ = 1;
%algs = {'rej', 'metro', 'gated'};
%colors = {'k', 'b', 'r'};
%numAlgs = size(algs, 2);
%%%%%%%%%

%%%%%%%%%
%dims = 6;
%cnstrs = 9;
%samples = 10000;
%numQ = 10;
%algs = {'rej', 'metro', 'gated', 'clever', 'full'};
%colors = {'k', 'b', 'r', 'g', 'c'};
%numAlgs = size(algs, 2);
%%%%%%%%%

data = cell([1, numAlgs]);
for i=1:numAlgs
    if analysisType == samples_fixed
        d = strcat(current_path, 'dims',  num2str(dims), '-cnstrs', num2str(cnstrs), '-samples', num2str(samples), '-q', num2str(numQ), '-', algs(i));
    else 
        d = strcat(current_path, 'dims',  num2str(dims), '-cnstrs', num2str(cnstrs), '-time', num2str(time_if_fixed), '-q', num2str(numQ), '-', algs(i));
    end
    data{i} = load(d{1});
end %for

%just to create semi log axis:
semilogx(data{1}(:,1),data{1}(:,2),'k-');
hold on;
for i=2:numAlgs
    c = [colors{i} '-'];
    semilogx(data{i}(:,1),data{i}(:,2),c);
end
hleg1 = legend(algs);
xlabel('no. samples')
ylabel('error')
for i=1:numAlgs
    %now error bounds (UP):   
    c = [colors{i} ':'];
    semilogx(data{i}(:,1),data{i}(:,2) + data{i}(:,3),c);
    %now error bounds (DOWN):   
    c = [colors{i} ':'];
    semilogx(data{i}(:,1),data{i}(:,2) - data{i}(:,3),c);
end %for
hold off;

%generate pdf:
eps_file = [current_path 'out.eps'];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
disp(eps_file);
system(['epstopdf ', eps_file]);
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%II. Time vs. Error:
vis = 'on';
figure('visible', vis);
reference_data_x = data{1}(:,4);
reference_data_y = data{1}(:,2);
%if analysisType == time_fixed &&  numel(reference_data_x)>time_if_fixed
%    reference_data_x = reference_data_x(1:time_if_fixed);
%    reference_data_y = reference_data_y(1:time_if_fixed);
%end %if
semilogy(reference_data_x,reference_data_y,'k-');
hold on;
for i=2:numAlgs
    c = [colors{i} '-'];
    semilogx(data{i}(:,4), data{i}(:,2),c);
end %for
hleg1 = legend(algs, 'Location','NorthEast');
xlabel('time (ms)')
ylabel('error')
hold off;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%III. Time:
time = cell([1, numAlgs]);
for i=1:numAlgs
    d = strcat(current_path, 'dims', num2str(dims), '-', algs(i), '-q', num2str(numQ));
    time{i} = load(d{1});
end %for
%t1 = strcat(current_path, 'dims', num2str(dims), '-', algs(1), '-q', num2str(numQ));
%t2 = strcat(current_path, 'dims', num2str(dims), '-', algs(2), '-q', num2str(numQ));
%t3 = strcat(current_path, 'dims', num2str(dims), '-', algs(3), '-q', num2str(numQ));
%time1 = load(t1{1}); %[current_path 'dims' num2str(dims) '-' algs(1) '-q' num2str(numQ)]);
%time2 = load(t2{1}); %[current_path 'dims' num2str(dims) '-' algs(2) '-q' num2str(numQ)]);
%time3 = load(t3{1}); %[current_path 'dims' num2str(dims) '-' algs(3) '-q' num2str(numQ)]);
vis = 'on';
figure('visible', vis);
semilogy(time{1}(:,1),time{1}(:,2),'k-');
hold on;
for i=2:numAlgs
    c = [colors{i} '-'];
    semilogx(time{i}(:,1),time{i}(:,2),c);
end %for
%semilogy(time1(:,1),time1(:,2),'k-',...
%    time2(:,1), time2(:,2),'b-',...
%    time3(:,1), time3(:,2),'r-');
hleg1 = legend(algs, 'Location','NorthWest');
xlabel('no. observed data')
ylabel('time (ms)')

