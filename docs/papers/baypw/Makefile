#================================================================
# Top level Makefile
#
# Author: Scott Sanner
# $Revision: 1.1 $
#================================================================

#
# main package:
PACKAGE = figs tex pdf wc

# Typical make targets:
all: $(PACKAGE)

# Packages:
tex: ALWAYS_BUILD
	m;
#	m; 
#	b; 
#	m; 

figs: ALWAYS_BUILD
	fig2dev -Leps mdp2.fig > mdp2.eps; 
	fig2dev -Leps mdp3.fig > mdp3.eps; 
	fig2dev -Leps int1d.fig > int1d.eps; 
	fig2dev -Leps int1d_v2.fig > int1d_v2.eps; 
	fig2dev -Leps int1d_v3.fig > int1d_v3.eps; 

pdf: ALWAYS_BUILD
	ps2pdf -sPAPERSIZE=letter -dMaxSubsetPct=100 -dCompatibilityLevel=1.2 -dSubsetFonts=false -dEmbedAllFonts=true baypw.ps
#	ps2pdf baypw.ps baypw.pdf; 

wc: ALWAYS_BUILD
	wc `find . -name "*.tex"`

clean:
	-rm *~; \
	rm baypw.dvi; rm baypw.log; \
	rm baypw.aux

# This is a dummy target that can be used to ensure that a target's
# dependencies are never satisfied.  This is useful because by default, make
# treats the presence of a directory with the same name as the target as an
# up-to-date target.
ALWAYS_BUILD:
