r = dlmread('../../uniform_preference_16.csv', ',');

% r = sort(r);

x = unique(r(:,1));
y = unique(r(:,2));
% z = r(:,3);
%mesh(x,y,r(:)); %mesh
% plot3(x,y,z,'-','MarkerSize',15) %nonunifor


R = zeros(length(x), length(y));
for i = 1 : length(x)
   for j = 1 : length(y)
       a = r(r(:,1)==x(i) & r(:,2)==y(j),:);
       if(length(a)>0)
           R(x(i),y(j)) = a(1,3); %r(r(:,1)==i & r(:,2)==j,:);
       end
   end
end

mesh(R);

xlabel ('Dimension', 'FontSize',24);
ylabel ('Constraints', 'FontSize',24);   
zlabel('Elapsed time (ms)', 'FontSize',24);

print -dpdf 'dim_constraint_time.pdf'
