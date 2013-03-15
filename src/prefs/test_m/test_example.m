n=1000;
x1 = zeros(n,1);
x2 = zeros(n,1);
last = 0;
rho = .1;

%when the joint is bivariate normal p(x1,x2) = N(0,[1,rho;rho,1])
for i = 1 : n
    x1(i) = rho*last + (1-rho*rho) * randn(1);
    x2(i) = rho*x1(i) + (1-rho*rho) * randn(1);
    last = x2(i);
end
x1 = x1(100:n,:);
x2 = x2(100:n,:);

% figure;
% plot(x1, '.')
% figure;
% plot(x2, '.')


a = csvread('../output.csv');
%a = a(100:length(a),:);

hold on
plot(x1,x2, 'b.')
plot(a(:,1),a(:,2), 'r.')
xlabel('x_1');
ylabel('x_2');
l=legend(['Gibbs sampled N(0,[1,' num2str(rho) ';' num2str(rho) ',1])'], 'Symbolic Gibbs');
% ,'interpreter','latex'
%\mathcal{N}(0,\begin{bmatrix}1,\rho\\ \rho,1\ebd{bmatrix})
set(l,'Interpreter','Latex');
hold off
s = ['gibbs' num2str(rho) '.pdf'];
print('-dpdf', s);