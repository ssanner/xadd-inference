# -*- coding: utf-8 -*-
"""
Created on Wed Oct 08 11:32:07 2014

@author: skinathil
"""

from mpl_toolkits.mplot3d import Axes3D
from matplotlib import cm
from matplotlib.ticker import LinearLocator, FormatStrFormatter
import matplotlib.pyplot as plt
import numpy as np

def plot_value_function(file_path):
    """
    Plot the value function
    """

    print "Plotting value function in {0}".format(file_path)

    # Check that the file exists

    data = np.genfromtxt('Book1.csv', delimiter=',', skip_header=1)

    xx = np.reshape(data[:,0], (50,50))
    yy = np.reshape(data[:,1], (50,50))
    zz = np.reshape(data[:,2], (50,50))

    fig = plt.figure()
    ax = fig.gca(projection='3d')

    surf = ax.plot_surface(xx, yy, zz, rstride=1, cstride=1, cmap=cm.coolwarm, linewidth=0, antialiased=False)

    fig.colorbar(surf, shrink=0.5, aspect=5)


    plt.savefig('output.pdf', format='pdf')
    plt.show()

if __name__ == "__main__":
    plot_value_function("C:\Users\skinathil\phd\dev\xadd-inference\trunk\src\cpomdp\market\utils\Book1.csv")