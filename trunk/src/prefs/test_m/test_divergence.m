
a = csvread('../output.csv');

hold on
plot(a(:,1),a(:,2), 'r.')
plot(a(:,3),a(:,4), 'b.')
xlabel('w_1');
ylabel('w_2');
%l=legend(['Gibbs sampled N(0,[1,' num2str(rho) ';' num2str(rho) ',1])'], 'Symbolic Gibbs');
%set(l,'Interpreter','Latex');
hold off
s = ['samples' '.pdf'];
print('-dpdf', s);