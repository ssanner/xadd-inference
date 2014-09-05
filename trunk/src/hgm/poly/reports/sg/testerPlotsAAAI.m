current_path = 'E:\REPORT_PATH_AAAI15\';

param = 5;
samples = 50000;
itr = 15; 
%%%%%%%%%

%algs = {'symbolic.gibbs', 'baseline.gibbs', 'rej', 'mh', 'tuned.mh'};
algs = {'symbolic.gibbs', 'baseline.gibbs', 'mh'};%, 'tuned.mh'};

colors = {'k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};
numAlgs = size(algs, 2);
%%%%%%%%%
%transparent = true;
doTest1 = true;
doTest2 = true;
doTest3 = false;

%%%%%%%%%

if doTest1    
% I. error vs. num. samples
figure;
info1 = cell([1, numAlgs]);
for i=1:numAlgs
    d = strcat(current_path, 'param',  num2str(param), '-itr', num2str(itr), '-', algs(i), '-samples', num2str(samples));
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
xlabel(strcat('no. samples (#param: ', num2str(param), ')'));
ylabel('error')
transparent = true;
for i=1:numAlgs
    c = [colors{i} ':'];
    A = transpose(info1{i}(:,1));
    B = transpose(info1{i}(:,2));
    C = transpose(info1{i}(:,3));
    if transparent
        %now error bounds (UP):   
        semilogx(A, B + C,c);
        %now error bounds (DOWN):   
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
        t = strcat(current_path, 'param',  num2str(param), '-itr', num2str(itr), '-', algs(i), '-times');%, num2str(times));
    info2{i} = load(t{1});
end %for

%just to create semi log axis:
%semilogy(info2{1}(:,2) * timeCoeff,info2{1}(:,3), [colors{1} '-']);%semilogx(data2{1}(:,2),data2{1}(:,3),'k-');
plot(info2{1}(:,2) * timeCoeff,info2{1}(:,3), [colors{1} '-']);%semilogx(data2{1}(:,2),data2{1}(:,3),'k-');
hold on;

for i=2:numAlgs
    c = [colors{i} '-'];
    if size(info2{i}, 2) >= 3
        semilogx(info2{i}(:,2) * timeCoeff,info2{i}(:,3),c);
    end %if
end
legend(algs);
xlabel(strcat('time(s) (#param: ', num2str(param), ')'))
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
%IV. Error vs parameter dim

vis = 'on';
figure('visible', vis);

info4 = cell([1, numAlgs]);
j = 1;
for i=1:numAlgs
    try 
        t = strcat(current_path, 'err-vs-param-', algs(i));
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
xlabel(strcat('param'));
ylabel(strcat('time to take: ', num2str(samples), ' samples'));
hold off;

end %do test4
%%%%%%%%%%%%


