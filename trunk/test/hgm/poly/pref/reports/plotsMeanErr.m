% taking the current path:
[current_path,name,ext] = fileparts(mfilename('fullpath'));
current_path = [current_path, '\'];

% parsing info file:


vis = 'on';
figure('visible', vis);
%hold on;

x = 0:1000;
y = log(x);

semilogx(x,y)

%%%%%%%%%
dims = 6;
cnstrs = 9;
samples = 10000;
numQ = 1;
algs = {'rej', 'metro', 'gated'};
%%%%%%%%%

d1 = strcat(current_path, 'dims',  num2str(dims), '-cnstrs', num2str(cnstrs), '-samples', num2str(samples), '-q', num2str(numQ), '-', algs(1));
d2 = strcat(current_path, 'dims',  num2str(dims), '-cnstrs', num2str(cnstrs), '-samples', num2str(samples), '-q', num2str(numQ), '-', algs(2));
d3 = strcat(current_path, 'dims',  num2str(dims), '-cnstrs', num2str(cnstrs), '-samples', num2str(samples), '-q', num2str(numQ), '-', algs(3));

data1 = load(d1{1});
data2 = load(d2{1});
data3 = load(d3{1});

%errorbar(data1(:,1),data1(:,2), data1(:,3));%,'-ko', 'LineWidth', 1);
%semilogx(abs(data1(:,2)))
semilogx(data1(:,1),data1(:,2),'k-',...
    data2(:,1),data2(:,2),'b-',...
    data3(:,1),data3(:,2),'r-',...          
     ...%now error bounds (UP):   
     data1(:,1),data1(:,2)+data1(:,3),'k:',...
     data2(:,1),data2(:,2)+data2(:,3),'b:',...
     data3(:,1),data3(:,2)+data3(:,3),'r:',...     
     ...%now error bounds (DOWN):          
     data1(:,1),abs(data1(:,2)-data1(:,3)),'k:',...
     data2(:,1),abs(data2(:,2)-data2(:,3)),'b:', ...
     data3(:,1),abs(data3(:,3)-data3(:,3)),'r:');

hleg1 = legend(algs);
xlabel('no. constraints')
ylabel('loss')

%hold off;
%xlabel('X', 'FontSize', font_size);
%ylabel('Y', 'FontSize', font_size);
%title(fig_title);

%generate pdf:

eps_file = [current_path 'out.eps'];
print('-depsc', eps_file);
%system(['sh convert_images.sh ' f]);
disp(eps_file);

system(['epstopdf ', eps_file]);

%%%%%%%%%%%%%%%%%%Time
t1 = strcat(current_path, 'dims', num2str(dims), '-', algs(1), '-q', num2str(numQ));
t2 = strcat(current_path, 'dims', num2str(dims), '-', algs(2), '-q', num2str(numQ));
t3 = strcat(current_path, 'dims', num2str(dims), '-', algs(3), '-q', num2str(numQ));

time1 = load(t1{1}); %[current_path 'dims' num2str(dims) '-' algs(1) '-q' num2str(numQ)]);
time2 = load(t2{1}); %[current_path 'dims' num2str(dims) '-' algs(2) '-q' num2str(numQ)]);
time3 = load(t3{1}); %[current_path 'dims' num2str(dims) '-' algs(3) '-q' num2str(numQ)]);

vis = 'on';
figure('visible', vis);
semilogy(time1(:,1),time1(:,2),'k-',...
    time2(:,1), time2(:,2),'b-',...
    time3(:,1), time3(:,2),'r-');
hleg1 = legend(algs);
xlabel('no. constraints')
ylabel('time')

