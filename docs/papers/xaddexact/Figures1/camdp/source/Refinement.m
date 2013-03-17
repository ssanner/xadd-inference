noP = [1    10   703;
    2   36  7968;
    3   102  147484;
    4   191  651187;
    5   245  37102453;
    6   381  50778063]

Res = [1   9   426;
    2   32  25029840;
    3   95  31226169]

iter = noP(:,1)

figure;
hold on;
xlabel('Horizon', 'FontSize', 16);
ylabel('Node Number', 'FontSize', 16);
title('Reservoir Management', 'FontSize', 16);
set(gca,'XTick',1:1:6);
%axes('FontSize', 16);
plot(1:6, noP(:,2), '-.r^');
plot(1:3, Res(:,2), '--b*');

legend('With LP-solver','With LP and SAT solvers',2);
hold off;


%%
figure;
semilogy(iter,noP(:,3), '--r*');
hold on;
semilogy(iter(1:3),Res(:,3), '-bo');
xlabel('Horizon', 'FontSize', 16);
ylabel('Time(ms)', 'FontSize', 16);
title('Reservoir Management', 'FontSize', 16);
set(gca,'XTick',1:1:6);
legend('With LP-solver','With LP and SAT solvers',2);
hold off;