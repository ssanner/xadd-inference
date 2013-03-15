import os

dir = '../results/';
files = [];
for f in os.listdir(dir):
     if f.startswith('results'):
          with open(dir + f) as file:               
               if file.read().find('# prefs:') == -1:
                    files.append(dir + f);
               
                    
for f in files:
     os.remove(f);
     

print 'Done!';