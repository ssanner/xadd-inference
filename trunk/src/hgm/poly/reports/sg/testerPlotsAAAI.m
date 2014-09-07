current_path = 'E:\REPORT_PATH_AAAI15\collision\';

param = 18;
samples = 1000;%1000;
itr = 2;%20; 
%%%%%%%%%

%algs = {'symbolic.gibbs', 'baseline.gibbs', 'rej', 'mh', 'tuned.mh'};
algs = {'symbolic.gibbs', 'baseline.gibbs', 'mh', 'tuned.mh', 'rej'};

colors = {'k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};
numAlgs = size(algs, 2);
%%%%%%%%%
%transparent = true;
doTestErr_vs_samples = false;
doTestErr_vs_times = true;
doTestSampleCount_vs_times = false;
doTestSampleEffectiveCount_vs_numSamples = false;%always false
doTest3 = true;

%%%%%%%%%

if doTestErr_vs_samples    
% I. error vs. num. samples

info1 = cell([1, numAlgs]);
for i=1:numAlgs
    d = strcat(current_path, 'param',  num2str(param), '-itr', num2str(itr), '-', algs(i), '-samples', num2str(samples));
    if exist(d{1}, 'file')
  % File exists.  Do stuff....
    info1{i} = load(d{1});
    end
end %for

artplot(info1, algs, colors, '#samples', 'error', 'plot', 'shaded', current_path, 'err-vs-samples');%'shaded'); %;'halo'); %'errorbar');

end %if do test 1
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
if doTestErr_vs_times
%II. Error vs. time:
info2 = cell([1, numAlgs]);
for i=1:numAlgs
        d = strcat(current_path, 'param',  num2str(param), '-itr', num2str(itr), '-', algs(i), '-times');%, num2str(times));
        if exist(d{1}, 'file')
            info2{i} = load(d{1});
        end
end %for
artplot(info2, algs, colors, 'time', 'error', 'plot', 'halo', current_path, 'err-vs-time');%'shaded'); %;'halo'); %'errorbar');

end %do test 2

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

if doTestSampleCount_vs_times

    infoN = cell([1, numAlgs]);

    for i=1:numAlgs
        d = strcat(current_path, 'param',  num2str(param), '-itr', num2str(itr), '-', algs(i), '-num4times');%, num2str(times));
        if exist(d{1}, 'file')
            infoN{i} = load(d{1});
        end
    end %for
    artplot(infoN, algs, colors, 'time', '# samples', 'semilogy', 'halo', current_path, 'samples-vs-time');%'shaded'); %;'halo'); %'errorbar');
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

if doTestSampleEffectiveCount_vs_numSamples
    infoN = cell([1, numAlgs]);
    for i=1:numAlgs
            d = strcat(current_path, 'param',  num2str(param), '-itr', num2str(itr), '-', algs(i), '-effective-samples', num2str(samples));
            if exist(d{1}, 'file')
                infoN{i} = load(d{1});
            end
    end %for
    artplot(infoN, algs, colors, 'effective-samples', '# samples', 'plot', 'halo', current_path, 'effective-samples');%'shaded'); %;'halo'); %'errorbar');
end


if doTest3
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%IV. Error vs parameter dim

info4 = cell([1, numAlgs]);
j = 1;
for i=1:numAlgs
       d = strcat(current_path, algs(i), '-toPassErrThr');
        if exist(d{1}, 'file')
            info4{i} = load(d{1});
        end
end %for
artplot(info4, algs, colors, 'parameter', 'time to pass threshold', 'semilogy', 'halo', current_path, 'time_vs_param');%'shaded'); %;'halo'); %'errorbar');
end %do test4
%%%%%%%%%%%%


