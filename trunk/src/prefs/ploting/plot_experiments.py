from matplotlib import rc
import matplotlib.pyplot as plt
from numpy import * 

x1 = arange(0, 2.5, .1);
y1 = -x1+2.5;
x2 = arange(2.5, 5.1, .1);
y2 = x2+2.5;

rc('text', usetex=True)
rc('font', family='serif')

fig = plt.figure()
ax = fig.add_subplot(111)
ax.plot(x1, y1, color='b')
ax.plot(x2, y2, color='g')
ax.set_xlabel('$x$');
ax.set_ylabel('$p(x)$');
plt.savefig('score_prior.pdf')