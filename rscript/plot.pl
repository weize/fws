use strict;

my @srcs = qw/annotator oracle01 oracle05 qd gmi plsa/;
my $model = 'fts';
for my $src(@srcs) {
	system("Rscript time-gain.R $src $model");
}
