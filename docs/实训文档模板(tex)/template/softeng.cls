% softeng.cls 软件工程文档统一模板类文件
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{softeng}[2026/07/11 Software Engineering Standard Doc Template]

% 基础文档基类：article，可换report/book
\LoadClass[a4paper,12pt]{article}

% 必用宏包统一加载，所有子文档无需重复导入
\RequirePackage{geometry}
\geometry{left=2.5cm,right=2.5cm,top=2.8cm,bottom=2.8cm}

% 中文支持（Overleaf/TeXLive通用）
\RequirePackage{ctex}

% ========== 修复点：替换废弃的ctexset章节配置，原生重定义章节格式 ==========
% section 一级标题：三号、加粗
\renewcommand{\section}{\@startsection{section}{1}{\z@}%
  {-3.5ex \@plus -1ex \@minus -.2ex}%
  {2.3ex \@plus.2ex}%
  {\bfseries\zihao{-3}}}

% subsection 二级标题：四号、加粗
\renewcommand{\subsection}{\@startsection{subsection}{2}{\z@}%
  {-3.25ex\@plus -1ex \@minus -.2ex}%
  {1.5ex \@plus .2ex}%
  {\bfseries\zihao{4}}}

% subsubsection 三级标题：小四号、加粗
\renewcommand{\subsubsection}{\@startsection{subsubsection}{3}{\z@}%
  {-3.25ex\@plus -1ex \@minus -.2ex}%
  {1.5ex \@plus .2ex}%
  {\bfseries\zihao{-4}}}

% 页眉页脚
\RequirePackage{fancyhdr}
\pagestyle{fancy}
\fancyhf{}
\fancyhead[C]{\leftmark}
\fancyfoot[C]{\thepage}
\renewcommand{\headrulewidth}{0.4pt}
\renewcommand{\footrulewidth}{0pt}
% 修复fancyhdr页眉高度过小警告
\setlength{\headheight}{12.64723pt}
\addtolength{\topmargin}{-0.64723pt}

% 目录格式
\RequirePackage{tocbibind}
\RequirePackage{tocloft}
\renewcommand{\cftsecfont}{\bfseries}
\renewcommand{\cftsubsecfont}{\normalfont}

% 代码块高亮（Java/PHP/Python/伪代码）
\RequirePackage{listings}
\RequirePackage{xcolor}
\lstset{
    basicstyle=\ttfamily\small,
    commentstyle=\color{gray},
    keywordstyle=\color{blue}\bfseries,
    stringstyle=\color{red},
    numbers=left,
    numberstyle=\tiny\color{gray},
    frame=single,
    breaklines=true,
    showstringspaces=false
}

% 图片、浮动环境
\RequirePackage{graphicx}
\RequirePackage{caption}
\captionsetup{font=small,labelfont=bf}
\graphicspath{{../figures/}} % 统一图片路径

% 表格增强
\RequirePackage{array,booktabs}


% 首行缩进2字符
\RequirePackage{indentfirst}
\setlength{\parindent}{2em}
% 段落间距
\setlength{\parskip}{0.3em}

% ========== 自定义全局命令（所有文档共用）==========
% 文档封面命令：参数1文档名称，参数2项目名称，参数3版本号
\newcommand{\doccover}[3]{
    \begin{titlepage}
        \centering
        \vspace*{3cm}
        \heiti\zihao{2}\bfseries #1 \par
        \vspace{1cm}
        \zihao{3}项目名称：#2 \par
        \vspace{2cm}
        \zihao{4}版本：#3 \par
        \vspace{3cm}
        \begin{tabular}{lc}
            成员： & 41234567 张三 \\ \cline{2-2}
            成员： & 41234567 张三 \\ \cline{2-2}
            成员： & 41234567 张三 \\ \cline{2-2}
            成员： & 41234567 张三 \\ \cline{2-2}
            成员： & 41234567 张三 \\ \cline{2-2}
        \end{tabular}\\[2cm]
        \vfill
        \zihao{-4}实训课程软件工程文档
    \end{titlepage}
}

% 修订历史记录环境
\newenvironment{revrecord}{
    \section*{版本修订记录}
    \begin{tabular}{|c|c|c|c|p{5cm}|}
    \hline
    版本 & 日期 & 修改人 & 审核 & 修改说明 \\
    \hline
}{
    \end{tabular}
    \newpage
}

% 需求条目快捷命令
\newcommand{\reqitem}[2]{\noindent \textbf{需求#1：} #2 \par}

% 超链接目录书签
\RequirePackage[colorlinks,linkcolor=black,citecolor=black,urlcolor=blue]{hyperref}

\endinput