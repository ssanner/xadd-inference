%% Copyright: National ICT Australia, 2005-2006
%% Author   : Alexander Smola
%%            Choon Hui Teo
%%            Le Song
%% License  : This file is licensed under the GNU Public License. See
%%            www.gnu.org for details on the license. 
%%           
%%            Original poster from Ulrike von Luxburg (MPIK)
%%            which was inspired by 
%%            http://www.physik.uni-augsburg.de/theo3/Comp/hp750c/computing_hp750c_A0.de.shtml
%%
%% This is a poster style for the NICTA TECHNICAL posters. Use it as
%% follows:
%%
%% \documentclass[landscape, a0]{nictalaymanlandscape1}
%% \columnfrac{0.5} %for two columns
%% \title{Title of the poster}
%% \subtitle{Subtitle of the poster}
%% \staff{Authors} % or \staff{Staff List}
%% \program{Name of the NICTA Program}
%% \authors{First Name $|$ Second Name $|$ Third Name $|$ Fourth Name ... }
%% \areaofinterest{Areas of interest of this program}
%%
%% \contentatitle{Title for content A. In other words, this is the
%%                LEFT half of this landscape poster}
%% \contenta{This is where the LEFT half of the content goes. It's a big minipage}
%%
%% \contentbtitle{Title for content B. In other words, this is the
%%                RIGHT half of this landscape poster}
%% \contentb{This is where the RIGHT half of the content goes. It's a big minipage}
%%
%% add columns with 
%%
%% \begin{pcolumn}
%% \section{Section level heading}
%% \subsection{Subsection level heading}
%%
%% whatever you want to write
%% \end{pcolumn}
%%
%% \notes{Put your notes on the bottom right corner of the poster}
%%
%% \begin{document}
%% \makeposter
%% \end{document}




\ProvidesClass{nictatechnical}[2006/04/20 v0.1 nictatechnical class]
\NeedsTeXFormat{LaTeX2e}
\LoadClass{article}

\usepackage{color}
\usepackage{geometry}
\usepackage{helvet}

\usepackage{multicol}
\usepackage{pst-grad}
\usepackage{graphicx}
\usepackage{subfig}

\usepackage{amssymb}
\usepackage{amsmath}
%\usepackage{bbold}

\newif\ifportrait
\newif\ifanullb
\newif\ifanull
\newif\ifaeins
\newif\ifazwei
\newif\ifadrei
\newif\ifdraft

\newcount\xkoord
\newcount\ykoord
\newcount\xscale
\newcount\yscale

\DeclareOption{a0b}{
\anullbtrue
\xkoord=2594 % big points (1 bp=1/72 inch)
\ykoord=3370 % big points (1 bp=1/72 inch)
\xscale=2
\yscale=2
}
\DeclareOption{a0}{
\anulltrue\anullbfalse
\xkoord=2380 % big points (1 bp=1/72 inch)
\ykoord=3368 % big points (1 bp=1/72 inch)
\xscale=2
\yscale=2
}
\DeclareOption{a1}{
\aeinstrue\anullbfalse
\xkoord=1684 % big points (1 bp=1/72 inch)
\ykoord=2380 % big points (1 bp=1/72 inch)
\xscale=3
\yscale=3
}
\DeclareOption{a2}{
\azweitrue\anullbfalse
\xkoord=1190 % big points (1 bp=1/72 inch)
\ykoord=1684 % big points (1 bp=1/72 inch)
\xscale=4
\yscale=4

}
\DeclareOption{a3}{
\adreitrue\anullbfalse
\xkoord=842  % big points (1 bp=1/72 inch)
\ykoord=1190 % big points (1 bp=1/72 inch)
\xscale=6
\yscale=6
}
\DeclareOption{landscape}{
\portraitfalse
}
\DeclareOption{portrait}{
\portraittrue
}
\DeclareOption{draft}{
\drafttrue
}
\DeclareOption{final}{
\draftfalse
}
\DeclareOption*{\PackageWarning{a0poster}{Unknown Option \CurrentOption}}
%\ExecuteOptions{landscape,a0b,final}
\ProcessOptions\relax

\ifanullb
  \geometry{papersize={119cm, 88cm}}
  \geometry{textwidth=116cm, textheight=88cm}
\else\ifanull
  \geometry{papersize={118.82cm, 83.96cm}}
  \geometry{textwidth=117.82cm, textheight=82.96cm}
\else\ifaeins
  \geometry{papersize={83.96cm, 59.4cm}}
  \geometry{textwidth=83.96cm, textheight=59.4cm}
\else\ifazwei
  \geometry{papersize={59.4cm, 41.98cm}}
  \geometry{textwidth=58.4cm, textheight=40.98cm}
\else\ifadrei
  \geometry{papersize={41.98, 29.7cm}}
  \geometry{textwidth=41.98cm, textheight=28.7cm}
\else\relax
                    \fi
               \fi
          \fi
     \fi
\fi

\ifportrait
%%    \newdimen\tausch
%%    \setlength{\tausch}{\paperwidth}
%%    \setlength{\paperwidth}{\paperheight}
%%    \setlength{\paperheight}{\tausch}
%%    \setlength{\tausch}{\textwidth}
%%    \setlength{\textwidth}{\textheight}
%%    \setlength{\textheight}{\tausch}
\geometry{landscape}
\else\relax
\fi

%% Setting proper dimensions for a DIN A0 printer

\setlength{\headheight}{0 cm}
\setlength{\headsep}{0 cm}
\setlength{\topmargin}{-10.7 mm}  % -1in +1.47cm
\setlength{\oddsidemargin}{-21.4 mm} % -1in +0.4cm


\catcode`\%=11
\newwrite\Ausgabe
\immediate\openout\Ausgabe=a0header.ps
\write\Ausgabe{%%BeginFeature *PageSize ISOA0/ISO A0}
\ifdraft\write\Ausgabe{2 dict dup /PageSize [595 842] put dup /ImagingBBox null put}
\else\write\Ausgabe{2 dict dup /PageSize [\number\xkoord\space \number\ykoord] put dup /ImagingBBox null put}\fi
\ifdraft\write\Ausgabe{38 84 translate}
\write\Ausgabe{0.\number\xscale\space 0.\number\yscale\space scale}\else\relax\fi
\write\Ausgabe{%%EndFeature}
\closeout\Ausgabe
\catcode`\%=14

%\special{header=./a0header.ps}

%\input{a0size.sty}

\renewcommand{\tiny}{\fontsize{12}{18}\selectfont}
\renewcommand{\scriptsize}{\fontsize{18.4}{22}\selectfont}
\renewcommand{\footnotesize}{\fontsize{20.28}{25}\selectfont}
\renewcommand{\small}{\fontsize{25.74}{30}\selectfont}
\renewcommand{\normalsize}{\fontsize{30.88}{35}\selectfont}
\renewcommand{\large}{\fontsize{35.86}{45}\selectfont}
\renewcommand{\Large}{\fontsize{43.83}{54}\selectfont}
\renewcommand{\LARGE}{\fontsize{51}{64}\selectfont}
\renewcommand{\huge}{\fontsize{61.6}{77}\selectfont}
\renewcommand{\Huge}{\fontsize{74.92}{93}\selectfont}
\newcommand{\veryHuge}{\fontsize{89.3}{112}\selectfont}
\newcommand{\VeryHuge}{\fontsize{107.16}{134}\selectfont}
\newcommand{\VERYHuge}{\fontsize{140}{140}\selectfont}

%% \setlength\smallskipamount{6pt plus 2pt minus 2pt}
%% \setlength\medskipamount{12pt plus 4pt minus 4pt}
%% \setlength\bigskipamount{24pt plus 8pt minus 8pt}

%% \setlength\abovecaptionskip{25pt}
%% \setlength\belowcaptionskip{0pt}
%% \setlength\abovedisplayskip{25pt plus 6pt minus 15 pt}
%% \setlength\abovedisplayshortskip{0pt plus 6pt}
%% \setlength\belowdisplayshortskip{13pt plus 7pt minus 6pt}
%% \setlength\belowdisplayskip\abovedisplayskip


\AtBeginDocument{
\ifanullb
   \ifportrait\special{papersize=91.51cm,120cm}\else\special{papersize=120cm,91.51cm}\fi
\else\ifanull
        \ifportrait\special{papersize=83.96cm,118.82cm}\else\special{papersize=118.82cm,83.96cm}\fi
     \else\ifaeins
             \ifportrait\special{papersize=59.4cm,83.96cm}\else\special{papersize=83.96cm,59.4cm}\fi
          \else\ifazwei
                  \ifportrait\special{papersize=41.98cm,59.4cm}\else\special{papersize=59.4cm,41.98cm}\fi
               \else\ifadrei
                      \ifdraft
                       \ifportrait\special{papersize=29.7cm,41.35cm}\else\special{papersize=41.98cm,29.0cm}\fi
                      \else
                       \ifportrait\special{papersize=29.7cm,41.98cm}\else\special{papersize=41.98cm,29.7cm}\fi
                      \fi
                    \else\relax
                    \fi
               \fi
          \fi
     \fi
\fi
\pagestyle{empty}}




%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%                pcolumn                       %%%             
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\newcommand{\thecolumnfrac}{.3}
\newcommand{\columnfrac}[1]{\renewcommand{\thecolumnfrac}{#1}}

\newenvironment{pcolumn}{%
  \begin{minipage}[t]{\thecolumnfrac\textwidth}\vspace{0.001mm}\addtolength{\parskip}{0.7em}}{\end{minipage}%
}


%%%%%%%%%% section commands %%%%%%%%%%%%%%%%%%%%%

\renewcommand{\section}[1]{
  \setlength{\unitlength}{\textwidth}
  \begin{picture}(1,0.1)
    \put(0,0.02){\color{nictapurple}\rule{\columnwidth}{0.06\columnwidth}}
    \put(0,0.036){\Large\bfseries\sffamily \color{white}\centerline{#1}}
  \end{picture}
}

\renewcommand{\subsection}[1]{
  \setlength{\unitlength}{\textwidth}
  \begin{picture}(1,0.06)
    \put(0,0.01){\color{nictablue}\rule{\columnwidth}{0.04\columnwidth}}
    \put(0,0.017){\large\bfseries\sffamily \color{white}\centerline{#1}}
  \end{picture}
}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%                colors                        %%%             
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\definecolor{cyan}{cmyk}{1,0,0,0}
\definecolor{magenta}{cmyk}{0,1,0,0}
\definecolor{yellow}{cmyk}{0,0,1,0}
\definecolor{red}{rgb}{0.8,0,0}
\definecolor{green}{rgb}{0,0.5,0}
\definecolor{black}{rgb}{0,0,0}
\definecolor{lightgrey}{rgb}{0.5,0.5,0.5}
\definecolor{white}{rgb}{1,1,1}
\definecolor{lightblue}{rgb}{0.7,0.7,1}
\definecolor{blue}{rgb}{0,0.387,0.816}
\definecolor{mine}{rgb}{1,1,1}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% nicta color palette
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\definecolor{nictablue}{cmyk}{1,0,0,0}
\definecolor{nictaviolet}{cmyk}{0.56,0.56,0,0}
\definecolor{nictayellow}{cmyk}{0,0.45,0.9,0}
\definecolor{nictapurple}{cmyk}{0.43,0.91,0,0}
\definecolor{nictablue}{cmyk}{1,0.6,0,0.06}
\definecolor{nictagreen}{cmyk}{0.2,0,0.94,0}
\definecolor{nictaslate}{cmyk}{0.235,0,0,0.79}
\definecolor{nictagrey}{cmyk}{0,0,0,0.15}

\definecolor{nictamint}{rgb}{0.313,0.961,0}
\definecolor{nictadgreen}{rgb}{0.0156,0.3945,0.3281}
\definecolor{nictagreen2}{rgb}{0.27,0.6,0.21}

\definecolor{nictapms342}{rgb}{0,0.316,0.219}
\definecolor{nictapms369}{rgb}{.383,0.672,0.117}

\newcommand{\Red}[1]{\color{red}{#1}\color{black}}
\newcommand{\Blue}[1]{\color{blue}{#1}\color{black}}
\newcommand{\Green}[1]{\color{green}{#1}\color{black}}
\newcommand{\Cyan}[1]{\color{cyan}{#1}\color{black}}
\newcommand{\Magenta}[1]{\color{magenta}{#1}\color{black}}
\newcommand{\Yellow}[1]{\color{yellow}{#1}\color{black}}
\newcommand{\Black}{\color{black}}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%                header                        %%%             
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


\newcommand{\nictatitle}{The greatest poster on earth}
\newcommand{\nictasubtitle}{}
\newcommand{\nictastaff}{Authors}
\newcommand{\nictanotes}{Notes are welcomed here...}
\newcommand{\nictaprogram}{}
\newcommand{\nictaauthors}{}
\newcommand{\nictacontent}{}

\renewcommand{\title}[1]{\renewcommand{\nictatitle}{#1}}
\newcommand{\subtitle}[1]{\renewcommand{\nictasubtitle}{#1}}
\newcommand{\staff}[1]{\renewcommand{\nictastaff}{#1}}
\newcommand{\program}[1]{\renewcommand{\nictaprogram}{#1}}
\newcommand{\authors}[1]{\renewcommand{\nictaauthors}{#1}}
\newcommand{\content}[1]{\renewcommand{\nictacontent}{{\fontencoding{OT1}  #1}}}
\newcommand{\notes}[1]{\renewcommand{\nictanotes}{#1}}
\newcommand{\makeposter}{
  \setlength{\unitlength}{\textwidth}
  
  \begin{picture}(0,0.0)
   
    %% Title
%    \put(0,-0.69){\includegraphics[width=\textwidth, height=\textheight]{nictalaymanlandscape1}}
    \put(-0.005,-0.695){\includegraphics[width=\textwidth, height=\textheight]{nictalaymanlandscape1}}
%    \put(0.0251,-0.0775){
    \put(0.0251,-0.0350){
      \begin{minipage}[c]{0.94\textwidth}
        \veryHuge\color{white}\sffamily\bfseries\nictatitle
      \end{minipage}
    }
    %\put(0.058,-0.255){\huge\sffamily\bfseries\nictaprogram} 

    %% Subtitle
    \put(0.025,-0.105){
      \begin{minipage}[t]{0.95\textwidth}
        \Large\color{nictagreen2}\sffamily\bfseries\nictasubtitle
      \end{minipage}
    }
      
    %% Authors
    \put(0.025,-0.105){
      \begin{minipage}[t]{0.95\textwidth}
        \raggedleft{\Large\sffamily\bfseries\nictaauthors}
      \end{minipage}
    } 
    
    %% Content
    \put(0.025,-0.115){
      \begin{minipage}[t]{0.95\textwidth}
        \nictacontent
      \end{minipage}
    }

    %% Notes
    \put(0.02,-0.673) {
      \begin{minipage}[t]{0.95\textwidth}
        \raggedleft{\Large\sffamily\bfseries\nictanotes}
      \end{minipage}
    }
  \end{picture}
}

\endinput
%% 
%% End of file `nictatechnical.cls'.
