noR = [1    5   39;
    2   16  323;
    3   27  1103;
    4   39  1859;
    5   39  2478;
    6   39  2892]

noRL = [1   5   26;
    2   27  221;
    3   259  1986;
    4   1839  93951]

RL = [1    5   61;
    2   14  535;
    3   22  1564;
    4   31  6083;
    5   30   11065;
    6   30   16343]

iter = noR(:,1)

figure;
hold on;
xlabel('Horizon', 'FontSize', 24);
ylabel('Node Number', 'FontSize', 24);
title('NonLinear Rover', 'FontSize', 24);
set(gca,'XTick',1:1:6);
 %axes('FontSize', 20);
plot(1:4, noRL(:,2), '-.r^');
plot(1:6, noR(:,2), '--b*');
plot(1:6, RL(:,2), '--g*');

legend('Without Pruning','Without Redundant Pruning', 'Inconsisent and Redundant Pruning',3);
hold off;


%%
figure;
semilogy(iter(1:4),noRL(:,3), '--r*');
hold on;
semilogy(iter,noR(:,3), '-bo');
semilogy(iter,RL(:,3), '-.ks');
xlabel('Horizon', 'FontSize', 24);
ylabel('Time(ms)', 'FontSize', 24);
%title('Inventory Control', 'FontSize', 16);
set(gca,'XTick',1:1:6);
legend('Without Pruning','Without Redundant Pruning', 'Inconsisent and Redundant Pruning',3);
hold off;