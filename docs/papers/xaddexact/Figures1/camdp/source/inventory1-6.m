SA1D = [1    5   47;
    2   7  203;
    3   7  437;
    4   5  577;
    5   5  702;
    6   5  811 ]

SA1S = [1   9   172;
    2   14  952;
    3   11  2013;
    4   17  2918;  
    5   20  5118;
    6   17  7006]

SA2D = [1    7   720;
    2   18  5016;
    3   43  12701;
    4   9  17069;
	5   7  18345;
	6   7  19407]

SA2S = [1   16   1459;
    2   38  18980;
    3   52  82101;
    4   79  253003;  
    5   21  490555;
    6   27  501219]

SA3D = [1    15   2180;
    2   16  35782;
    3   32  100974;
    4   39  627762;
	5   18  757605;
	6   24  904707]

E = [12 267;
     203  15099;
     Inf Inf];



hold on;
xlabel('Horizon', 'FontSize', 16);
ylabel('Size of V^h (Nodes)', 'FontSize', 16);
title('Inventory Control', 'FontSize', 16);
set(gca,'XTick',1:1:6);
% axes('FontSize', 16);
plot(1:6, SA1D(:,2), '--r*');
plot(1:6, SA1S(:,2), ':ro');
plot(1:6, SA2D(:,2), '--g*','linewidth',2);
plot(1:6, SA2S(:,2), ':go','linewidth',2);
plot(1:6, SA3D(:,2), '-b*');
plot(1:3, E(:,1), '-.ks');

legend('1 Item, DD ','1 Item, SD ', '2 Item,DD','2 Item, SD', '3 Item, DD','1 Item, DD,no pruning', 6, 'FontSize', 16);
hold off;


%%

figure;
semilogy(1:6,SA1D(:,3), '--r*');
hold on;
semilogy(1:6,SA1S(:,3), ':ro');
semilogy(1:6,SA2D(:,3), '--g*','linewidth',2);
semilogy(1:6, SA2S(:,3), ':go','linewidth',2);
semilogy(1:6,SA3D(:,3), '-b*');
semilogy(1:6,E(:,2), '-.ks');
xlabel('Horizon', 'FontSize', 16);
ylabel('Time(ms)', 'FontSize', 16);
title('Inventory Control', 'FontSize', 16);
set(gca,'XTick',1:1:6);
legend('1 Item, DD ','1 Item, SD ', '2 Item,DD','2 Item, SD', '3 Item, DD','1 Item, DD,no pruning', 6, 'FontSize', 16);
hold off;