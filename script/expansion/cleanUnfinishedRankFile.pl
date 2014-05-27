use strict;
require 'config.pl';
my @files = <../java/tmp/stderr/galago-process-*>; 

for my $file (@files) {
	print $file."\n";
	process($file);
}

sub process {
	my $file = shift;
	my $in = openReader($file);

	my @processing;
	my %written = (); 

	while(my $line = <$in>) {
		chomp $line;
		if ($line =~ m/^Processing (\S+)$/) {
			push @processing, $1;
		}
		if ($line =~ m/^Writte in (\S+)$/) {
			$written{$1} = 1;
		}
	}
	close($in);
	
	for my $p (@processing) {
		if(!exists $written{$p}) {
			my $cmd = "rm $p";
		#	print "$cmd\n";
			call($cmd);
		}
	}
}
