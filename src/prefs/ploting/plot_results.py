from numpy import *
import matplotlib.pyplot as plt
import math
import os


dataset = 'score'#'pref';

if dataset == 'pref':
    # 'results_2013-2-1-2-23-43.txt'
    filenames=['results_2013-1-28-10-40-50.txt',
                'results_2013-1-27-1-56-18.txt', 
                'results_2013-1-26-7-30-55.txt', 
                'results_2013-1-26-7-57-7.txt', 
                'results_2013-1-27-0-51-29.txt', 
                'results_2013-1-27-0-40-14.txt'];
elif dataset == 'score':
    filenames = ['results_2013-2-1-8-8-56.txt',
                 'results_2013-2-1-8-7-22.txt'] #  'results_2013-2-2-0-30-37.txt'];



cdfs = [];
exact_time = [];
sampling_time = [];
pref_count = [];
no_items = [];
no_exact_items = [];
pref_count_exact = [];
no_nodes = [];
no_exact_nodes = [];

for s in filenames:
    with open('../results/' + s) as f:
        for line in f:
            if line.startswith('-------') and len(line) < 10:
                break;
            
            if line.find('Time') > -1 and line.find('CDF') > -1:
                cdfs.append(float(line[line.find(':')+2:]));      
                
            if line.find('Time') > -1 and line.find('samples:') > -1:
                sampling_time.append(float(line[line.find(':')+2:]));                
                
            if line.find('# preferences') > -1:
                pref_count.append(int(line[line.find(':')+2:]));   
                
            if line.find('# items') > -1:
                no_items.append(int(line[line.find(':')+2:]));                                                
                
            idx = line.find('no_nodes:');
            if idx > -1:
                l = line[idx:];
                no_nodes.append(int(l[l.find(':')+2:l.find(',')]));                
                
            idx = line.find('inference_time_exact:');
            if idx > -1:
                l = line[idx:];
                exact_time.append(float(l[l.find(':')+2:l.find(',')]));            

            idx = line.find('no_exact_nodes:');
            if idx > -1:
                l = line[idx:];
                print s;
                no_exact_nodes.append(int(l[l.find(':')+2:l.find(',')]));                                            
                pref_count_exact.append(no_nodes[len(no_nodes)-1]);                
                no_exact_items.append(int(no_items[len(no_items)-1]));


# sorting facilities for the items based on the number provided
def sort_items(items):
    srt = sorted(items, key=int) # 
    idx = [];
    for a in srt:
        idx.append(items.index(a));
    return idx;

def get_elements(idx, col):
    ret_val = [];
    for i in idx:
        ret_val.append(col[i]);
    return ret_val;

# sort based on the number of items
idx=sort_items(no_items);
no_items=get_elements(idx, no_items);
cdfs=get_elements(idx, cdfs);
sampling_time=get_elements(idx, sampling_time);
pref_count=get_elements(idx, pref_count);
no_nodes=get_elements(idx, no_nodes);

idx=sort_items(no_exact_items);
no_exact_items = get_elements(idx,no_exact_items)
pref_count_exact=get_elements(idx, pref_count_exact);
no_exact_nodes=get_elements(idx, no_exact_nodes);
exact_time=get_elements(idx, exact_time);
# sorting facilities are finished here :D

print 'cdfs:            ', cdfs;
print 'sampling_time:   ', sampling_time;
print 'no_items:        ', no_items;
print 'pref_count:      ', pref_count;
print 'no_nodes:        ', no_nodes;
print '-------';
print 'no_exact_items:  ', no_exact_items;
print 'pref_count_exact:', pref_count_exact
print 'no_exact_nodes:  ', no_exact_nodes;
print 'exact_time:      ', exact_time;

# plotting results here
plt.rcParams['font.size'] = 24
fig = plt.figure()
ax = fig.add_subplot(111)
ax.plot(no_items, sampling_time, color='b', linewidth=5, label='Sample Generation')
ax.plot(no_items, cdfs, color='g', linewidth=5, label='Building CDFs')
ax.plot(no_exact_items, exact_time, color='r', linewidth=5, label='Exact Inference')
ax.set_yscale('log');
ax.set_xlabel('No. Items');
ax.set_ylabel('Time (ms)');
plt.legend(loc="upper right", bbox_to_anchor=[.55, 1], borderaxespad=0., fontsize=18);
plt.savefig('time.pdf')

fig = plt.figure()
ax = fig.add_subplot(111)
ax.plot(no_items, no_nodes, color='b', linewidth=5)
ax.plot(no_exact_items, no_exact_nodes, color='r', linewidth=5)
ax.set_yscale('log');
ax.set_xlabel('No. Items');
ax.set_ylabel('No. XADD Nodes');
plt.savefig('nodes.pdf')

os.system('cp time.pdf ../../../docs/papers/baypw/images/time_' + dataset + '.pdf')
os.system('cp nodes.pdf ../../../docs/papers/baypw/images/nodes_' + dataset + '.pdf')