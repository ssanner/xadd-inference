% plot_local('1')
function [] = plot_local(id) %, subfig)

fontsize = 30;

cond{1} = 'x_2=6, x_1=5';
cond{2} = 'x_2=8, x_1=5';
cond{3} = 'x_2=5, x_1=3, x_3=8';
cond{4} = 'x_2=5, x_1=5, x_3=6';
cond{5} = 'x_2=5, x_1=3, x_4=4, x_3=8';
cond{6} = 'x_2=5, x_1=4, x_4=6, x_3=5';

str = ['load tracking.query.',id,'.txt']
eval(['load tracking.query.',id,'.txt']);
d = tracking;

figure(1);

%if (subfig > 0)
%    subplot(2,3,subfig);
%end

hold off
plot(d(101:201,1),d(101:201,2), 'b')
hold on

%cond{str2num(id)}
ylabel(['P(D=d|','...',')'],'FontSize',fontsize)
xlabel('d','FontSize',fontsize) + 6

axis tight;
axis([0,10,-.1,max(d(101:201,2)) + .1]);

print(gcf, '-depsc', ['tracking_',id,'.eps']);
fixPSlinestyle(['tracking_',id,'.eps'],['r',id,'.eps']);


