use strict;

my @dirs = <facet-tune-gm-ll-sample-f10-*>;
for my $dir (@dirs) {
	my $run = "run";
	if ($dir =~ m/facet-tune-gm-ll-sample-f10-(.*)/) {
		$run = $1;
	}
	my $cmd = "perl ../script/facet/eval-grep-all-tuned-result-measurecmb.pl $run $dir > $dir/eval";
	print "$cmd\n";
	system($cmd);
}


