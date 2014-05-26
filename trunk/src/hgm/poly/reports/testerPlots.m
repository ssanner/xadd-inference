%OK
% taking the current path:
%[current_path,name,ext] = fileparts(mfilename('fullpath'));
%current_path = [current_path, '\'];
current_path = 'E:\REPORT_PATH\';

% parsing info file:


%%vis = 'on';
%%figure('visible', vis);
%hold on;
%x = 0:1000;
%y = log(x);
%semilogx(x,y)

%samples_fixed = 1;
%time_fixed = 2;

%%%%%%%%%
%analysisType = time_fixed; %time_fixed; %samples_fixed; %time_fixed;
dims = 10;%8;
cnstrs = 10;
samples = 10000;
%times = 50;
%maxT = 60000;
numQ = 20;  %f
%%%%%%%%%
fixed_dim = dims;
fixed_data = cnstrs;

%algs = {'rej.general.bppl', 'mh.general.bppl', 'targeted.gated.gibbs.const.bppl', 'gated.gibbs.const.bppl', 'rej.original.bppl', 'mh.general.bppl2', 'gated.gibbs.general.bppl'};%{'gated.gibbs.const.bppl', 'mh.general.bppl', 'mh.general.bppl2','targeted.gated.gibbs.const.bppl'};%, 'full.gibbs.general.bppl'}; % , 'full.gibbs.bayes.mmm'}; %{'gated', 'gated.bayes'};
%algs = {'rej.general.bppl', 'mh.general.bppl', 'targeted.gated.gibbs.const.bppl', 'gated.gibbs.const.bppl', 'rej.original.bppl', 'mh.general.bppl2', 'gated.gibbs.general.bppl', 'full.gibbs.const.bppl'};%{'gated.gibbs.const.bppl', 'mh.general.bppl', 'mh.general.bppl2','targeted.gated.gibbs.const.bppl'};%, 'full.gibbs.general.bppl'}; % , 'full.gibbs.bayes.mmm'}; %{'gated', 'gated.bayes'};
%algs = {'rej.general.bppl', 'mh.general.bppl', 'targeted.gated.gibbs.const.bppl', 'gated.gibbs.const.bppl', 'gated.gibbs.general.bppl', 'full.gibbs.const.bppl'};%{'gated.gibbs.const.bppl', 'mh.general.bppl', 'mh.general.bppl2','targeted.gated.gibbs.const.bppl'};%, 'full.gibbs.general.bppl'}; % , 'full.gibbs.bayes.mmm'}; %{'gated', 'gated.bayes'};
algs = {'targeted.gated.gibbs.general.mmm', 'mh.general.mmm', 'rej.general.mmm', 'gated.gibbs.general.mmm',  'full.gibbs.general.mmm', 'rej.original.mmm'};
colors = {'k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};
numAlgs = size(algs, 2);
%%%%%%%%%
transparent = true;
doTest1 = true;
doTest2 = true;
doTest3 = false;
doTest4 = false;

%%%%%%%%%

if doTest1
% I. error vs. num. samples
info1 = cell([1, numAlgs]);
for i=1:numAlgs
    %if analysisType == samples_fixed
        d = strcat(current_path, 'dim',  num2str(dims), '-data', num2str(cnstrs), '-f', num2str(numQ), '-', algs(i), '-samples', num2str(samples));
    %else 
    %    d = strcat(current_path, 'dims',  num2str(dims), '-cnstrs', num2str(cnstrs), '-time', num2str(maxT), '-q', num2str(numQ), '-', algs(i));
    %end
    info1{i} = load(d{1});
end %for

%just to create semi log axis:
loglog(info1{1}(:,1),info1{1}(:,2),[colors{1} '-']);
hold on;
for i=2:numAlgs
    c = [colors{i} '-'];
    semilogx(info1{i}(:,1),info1{i}(:,2),c);
end
legend(algs);
xlabel(strcat('no. samples (#data: ', num2str(cnstrs), ', #dim: ', num2str(dims), ')'));
ylabel('error')
for i=1:numAlgs
    c = [colors{i} ':'];
    A = transpose(info1{i}(:,1));
    B = transpose(info1{i}(:,2));
    C = transpose(info1{i}(:,3));
    if transparent
        %now error bounds (UP):   
        %semilogx(info1{i}(:,1),info1{i}(:,2) + info1{i}(:,3),c);
        semilogx(A, B + C,c);
        %now error bounds (DOWN):   
        %semilogx(info1{i}(:,1),max(info1{i}(:,2) - info1{i}(:,3),0),c);
        semilogx(A, max (B - C, 0), c);
    else
    %if C is negative the logarithmic diagram becomes crazy....
    C0 = 0.999*(B - max(B - C, 0));
    shadedErrorBar(A,B,C0,c);
    end %if transparent
end %for
hold off;

%generate pdf:
eps_file = [current_path 'out.eps'];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
disp(eps_file);
system(['epstopdf ', eps_file]);

end %if do test 1
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
if doTest2
%II. Error vs. time:

timeCoeff = 0.001; %to convert ms to s
vis = 'on';
figure('visible', vis);

info2 = cell([1, numAlgs]);
for i=1:numAlgs
        t = strcat(current_path, 'dim',  num2str(dims), '-data', num2str(cnstrs), '-f', num2str(numQ), '-', algs(i), '-times');%, num2str(times));
    info2{i} = load(t{1});
end %for



%just to create semi log axis:
semilogy(info2{1}(:,2) * timeCoeff,info2{1}(:,3), [colors{1} '-']);%semilogx(data2{1}(:,2),data2{1}(:,3),'k-');
hold on;

for i=2:numAlgs
    c = [colors{i} '-'];
    if size(info2{i}, 2) >= 3
        semilogx(info2{i}(:,2) * timeCoeff,info2{i}(:,3),c);
    end %if
end
legend(algs);
xlabel(strcat('time(s) (#data: ', num2str(cnstrs), ', #dim: ', num2str(dims), ')'))
ylabel('error')
for i=1:numAlgs
    c = [colors{i} ':'];   
    if size(info2{i}, 2) >= 4
        % first entry is just a counter is not used
        A = transpose(info2{i}(:,2) * timeCoeff); %time
        B = transpose(info2{i}(:,3)); %mean err
        C = transpose(info2{i}(:,4)); %stderr of err
        if transparent
            semilogx(A, B+C, c);
            semilogx(A, max(B-C,0), c);
        %now error bounds (UP):   
        %semilogx(info2{i}(:,2),info2{i}(:,3) + info2{i}(:,4),c);%semilogx(data2{i}(:,2),data2{i}(:,3) + data2{i}(:,4),c);
        %now error bounds (DOWN):   
        %semilogx(info2{i}(:,2), max(info2{i}(:,3) - info2{i}(:,4), 0),c);%semilogx(data2{i}(:,2),data2{i}(:,3) - data2{i}(:,4),c);
        else
        %if C2 is negative the logarithmic diagram becomes crazy....
            C0 = 0.999*(B - max(B - C, 0));
            shadedErrorBar(A,B,C0,c);
        end % if transparent
    end %if
end %for
hold off;
end %do test 2

if doTest3
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%III. Dim is Fixed. Error vs data

vis = 'on';
figure('visible', vis);

info3 = cell([1, numAlgs]);
for i=1:numAlgs
        %TODO: fixed
        t = strcat(current_path, 'fixed-dim',  num2str(fixed_dim), '-', algs(i));
    info3{i} = load(t{1});
end %for

%just to create semi log axis:
semilogy(info3{1}(:,1),info3{1}(:,2),[colors{1} '-']);

hold on;
for i=2:numAlgs
    c = [colors{i} '-'];
    semilogy(info3{i}(:,1), info3{i}(:,2), c);
end
legend(algs);
xlabel(strcat('data (#dims: ', num2str(fixed_dim), ')'));
ylabel(strcat('time to take: ', num2str(samples), ' samples'));
hold off;
end %if do test3

if doTest4

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%IV. Observed Data is Fixed. Error vs parameter dim

vis = 'on';
figure('visible', vis);

info4 = cell([1, numAlgs]);
j = 1;
for i=1:numAlgs
    try 
        t = strcat(current_path, 'fixed-data',  num2str(fixed_data), '-', algs(i));
        info4{j} = load(t{1});
        j = j+1;
        j
    catch exception
    getReport(exception)   
    end %end try    
end %for
j = j-1;

if j>0
%just to create semi log axis:
%semilogy(info4{1}(:,1),info4{1}(:,2),'k-');
plot(info4{1}(:,1),info4{1}(:,2),[colors{1} '-']);
end %if

hold on;
if j>1
for i=2:j%numAlgs
    c = [colors{i} '-'];
    semilogy(info4{i}(:,1), info4{i}(:,2), c);
end
end %if j>1
hleg1 = legend(algs);
xlabel(strcat('dim (#data: ', num2str(fixed_data), ')'));
ylabel(strcat('time to take: ', num2str(samples), ' samples'));
hold off;

end %do test4
%%%%%%%%%%%%


