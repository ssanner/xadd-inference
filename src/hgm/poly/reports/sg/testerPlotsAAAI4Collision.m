current_path = 'E:\REPORT_PATH_AAAI15\collision\';

%numParams = [4, 30]; %[3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30];
numParams = [30]; %[3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30];

%param = 18;
samples = 200;%1000;%1000;
itr = 10;%20; 
%%%%%%%%%

%algs = {'symbolic.gibbs', 'baseline.gibbs', 'mh', 'tuned.mh', 'rej'};
%alg_titles = {'Symbolic Gibbs', 'Baseline Gibbs', 'MH', 'Tuned MH', 'Rejection'};
%colors = {'k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};

algs = {'rej', 'tuned.mh','mh','baseline.gibbs', 'symbolic.gibbs'};
alg_titles = {'Rejection', 'Tuned MH','MH', 'Baseline Gibbs', 'Symbolic Gibbs'};
colors = {'c', 'g','r','b','k','k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};

algs = {'tuned.mh','mh','baseline.gibbs', 'symbolic.gibbs'};
alg_titles = {'Tuned MH','MH', 'Baseline Gibbs', 'Symbolic Gibbs'};
colors = {'g','r','b','k','k', 'b', 'r', 'g', 'c', 'm', 'y', 'b'};


numAlgs = size(algs, 2);
%%%%%%%%%
doTestErr_vs_samples = false;
doTestErr_vs_times = true;
doTestSampleCount_vs_times = false;
doTestSampleEffectiveCount_vs_numSamples = false;%always false
doTestTimeToPassGoldenErrThreshold_vs_param = false;
doTestTimeToTake100Samples = false;

for param = numParams
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

artplot(info1, alg_titles, colors, strcat('#Samples (size=', num2str(param), ')'), 'Error', 'plot', 'halo', current_path, strcat('err-vs-samples__param', num2str(param)));%'shaded'); %;'halo'); %'errorbar');

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
artplot(info2, alg_titles, colors, strcat('Wall-clock time (size=', num2str(param), ')'), 'Error', 'plot', 'shaded', current_path, strcat('err-vs-time__param', num2str(param)));%'shaded'); %;'halo'); %'errorbar');

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
    artplot(infoN, alg_titles, colors, strcat('time (size=', num2str(param), ')'), '# samples', 'semilogy', 'halo', current_path, strcat('samples-vs-time__param', num2str(param)));%'shaded'); %;'halo'); %'errorbar');
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
    artplot(infoN, alg_titles, colors, strcat('effective-samples size=', num2str(param), ')'), '# samples', 'plot', 'halo', current_path, strcat('effective-samples__param', num2str(param)));%'shaded'); %;'halo'); %'errorbar');
end

end %param

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

if doTestTimeToPassGoldenErrThreshold_vs_param
%IV. Error vs parameter dim
info4 = cell([1, numAlgs]);
for i=1:numAlgs
       d = strcat(current_path, algs(i), '-toPassErrThr');
        if exist(d{1}, 'file')
            info4{i} = load(d{1});
        end
end %for
artplot(info4, alg_titles, colors, 'size', 'wall-clock time to pass threshold', 'semilogy', 'errorbar', current_path, 'time_vs_param');%'shaded'); %;'halo'); %'errorbar');
end %do test4

%%%%%%%%%%%%

if doTestTimeToTake100Samples
    %time to take 100 samples vs. params
    info5 = cell([1, numAlgs]);

    for i=1:numAlgs
       d = strcat(current_path, algs(i), '-toTake100samples');
        if exist(d{1}, 'file')
            info5{i} = load(d{1});
        end   
    end %for
artplot(info5, alg_titles, colors, 'size', 'time to take 100 samples', 'semilogy', 'halo', current_path, 'toTake100samples_vs_param');%'shaded'); %;'halo'); %'errorbar');

end


