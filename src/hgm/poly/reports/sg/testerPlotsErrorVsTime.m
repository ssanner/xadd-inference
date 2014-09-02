current_path = 'E:\REPORT_PATH_AAAI15\symmetric\';

param = 28;
samples = 100;
itr = 1; 
%%%%%%%%%

algs = {'symbolic.gibbs', 'rej', 'mh'};%, 'tuned.mh', 'baseline.gibbs'};
colors = {'k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};
numAlgs = size(algs, 2);
%%%%%%%%%

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
semilogy(info2{1}(:,2) * timeCoeff,info2{1}(:,3), [colors{1} '-']);%semilogx(data2{1}(:,2),data2{1}(:,3),'k-');
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
transparent = true;
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







