obj2size = 1; %2 for collision 1 otherwis.
%current_path = 'E:\REPORT_PATH_ICML15\collision\arcGood3\';%'E:\REPORT_PATH_ICML15\conductance\arc_good\'; %'E:\REPORT_PATH_ICML15\collision\arcGood3\'; 
%current_path = 'E:\REPORT_PATH_ICML15\conductance\arc_good\'; %'E:\REPORT_PATH_ICML15\collision\arcGood3\'; 
current_path = 'E:\REPORT_PATH_ICML15\conductance\';%arc_good\';%'E:\REPORT_PATH_ICML15\collision\arc4\';%'E:\REPORT_PATH_ICML15\conductance\arc_good\'; %'E:\REPORT_PATH_ICML15\collision\arcGood3\'; 

numParams = [30 ];%[2, 3, 4, 5, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34];%[5];%[2, 4, 5, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30];
samples = 200;%1000;%1000;
itr = 15;%10;
%%%%%%%%%

%algs =       {'anglican.rdb', 'anglican.smc', 'anglican.pgibbs', 'tuned.mh', 'baseline.gibbs', 'rej',       'symbolic.gibbs',};
%alg_titles = {'anglican.rdb', 'anglican.smc', 'anglican.pgibbs', 'tuned.mh', 'baseline.gibbs', 'rejection', 'Symbolic Gibbs'};

%algs =       {'anglican.rdb', 'anglican.smc', 'anglican.pgibbs', 'tuned.mh', 'baseline.gibbs', 'symbolic.gibbs',};
%alg_titles = {'anglican.rdb', 'anglican.smc', 'anglican.pgibbs', 'tuned.mh', 'baseline.gibbs', 'Symbolic Gibbs'};

%algs =       {'anglican.rdb', 'anglican.smc',  'stan.hmc','tuned.mh', 'baseline.gibbs', 'rej', 'symbolic.gibbs'};%, 'stan.hmc', 'rej'};
%alg_titles = {'RDB*',          'SMC*',         'HMC*',    'MH',      'Gibbs',          'Rej',  'SymGibbs'};%,       'HMC', 'Rej'};

algs =       {'baseline.gibbs',  'anglican.smc','tuned.mh',  'stan.hmc', 'rej',  'symbolic.gibbs'};%, 'stan.hmc', 'rej'};
alg_titles = {'BaseGibbs'     ,  'SMC',         'MH',      'HMC',       'Rej',  'SymGibbs'};%,       'HMC', 'Rej'};
colors =     {'g',               'b',           'm',       'r',              'c',  'k', 'g', 'c', 'm', 'y', 'b'};

%conductance err vs size
%algs =       {'tuned.mh',  'rej', 'baseline.gibbs',  'symbolic.gibbs'};%, 'stan.hmc', 'rej'};
%alg_titles = {'MH',        'Rej', 'BaseGibbs'     ,  'SymGibbs'};%,       'HMC', 'Rej'};
%colors =     {'m',         'c',   'g',               'k', 'g', 'c', 'm', 'y', 'b'};
algs =       {'tuned.mh', 'anglican.smc', 'stan.hmc', 'baseline.gibbs', 'symbolic.gibbs'};%, 'stan.hmc', 'rej'};
alg_titles = {'MH',       'SMC',          'HMC',      'BaseGibbs'    , 'SymGibbs'};%,       'HMC', 'Rej'};
colors =     {'m',        'b',            'r',        'g', 'k', 'g', 'c', 'm', 'y', 'b'};


%without Rej:
%algs =       {'baseline.gibbs',  'anglican.smc','tuned.mh',  'stan.hmc', 'symbolic.gibbs'};%, 'stan.hmc', 'rej'};
%alg_titles = {'BaseGibbs'     ,  'SMC',         'MH',      'HMC',        'SymGibbs'};%,       'HMC', 'Rej'};
%colors =     {'g',               'b',           'm',       'r',          'k', 'g', 'c', 'm', 'y', 'b'};

%for little visual collisoin..
%algs =       {'smc1' ,   'smc2' ,   'stan1',   'stan2',   'baseline.gibbs',  'tuned.mh',  'symbolic.gibbs',     'rej'};%, 'stan.hmc', 'rej'};
%alg_titles = {'SMC (e)', 'SMC (f)', 'HMC (c)', 'HMC (d)', 'BaseGibbs',    'MH (b)',                'SymGibbs (a)',    'Rej'};%,       'HMC', 'Rej'};
%colors = {    'b',       'b',       'r',       'r',       'g',                'm',                   'k',          'y', 'c', 'y', 'b'};


numAlgs = size(algs, 2);
%%%%%%%%%
doTestErr_vs_samples = false;
doTestErr_vs_times = true;
doTestSampleCount_vs_times = false;
doTestSampleEffectiveCount_vs_numSamples = false;%always false
doTestTimeToPassGoldenErrThreshold_vs_param = true;
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
            info2{i}(:,2) = info2{i}(:,2) * 1.0e-9; % converting nano seconds to seconds
        else
            % error(strcat('Does NOT exist... ', d{1}))
            ermsg = strcat('Does NOT exist... ', d{1})
            info2{i} = [0,0,0,0; 0,0,0,0];
        end
end %for
%artplot(info2, alg_titles, colors, strcat('Wall-clock time (s) (size=', num2str(param), ')'), 'Error', 'plot', 'shaded', current_path, strcat('err-vs-time__param', num2str(param)));%'shaded'); %;'halo'); %'errorbar');
artplot(info2, alg_titles, colors, strcat('wall-clock time (s) (size=', num2str(param*obj2size), ')'), 'absolute error', 'plot', 'shaded', current_path, strcat('err-vs-time__param', num2str(param)));%'shaded'); %;'halo'); %'errorbar');

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
info4 = cell(1,1);%cell([1, numAlgs]);
good_ald_counter = 0;
for i=1:numAlgs
       d = strcat(current_path, algs(i), '-toPassErrThr');
        if exist(d{1}, 'file')
            good_ald_counter = good_ald_counter+1;
            infoo = load(d{1});
            infoo(:,2) = infoo(:,2) * obj2size;
            infoo(:,3) = infoo(:,3) * 1.0e-9;
            infoo(:,4) = infoo(:,4) * 1.0e-9;
            info4{good_ald_counter} = infoo;            
            good_alg_titles(good_ald_counter) = alg_titles(i);
            good_colors(good_ald_counter) = colors(i);
            
        else
            ermsg = strcat('Does NOT exist... ', d{1})
        end
end %for

artplot(info4, good_alg_titles, good_colors, 'size', 'time(s) to pass threshold \tau=0.045', 'semilogy', 'errorbar', current_path, 'time_vs_param');%'shaded'); %;'halo'); %'errorbar');
%artplot(info4, good_alg_titles, good_colors, 'size', 'time (s) to pass threshold \tau=0.3', 'semilogy', 'errorbar', current_path, 'time_vs_param');%'shaded'); %;'halo'); %'errorbar');
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


