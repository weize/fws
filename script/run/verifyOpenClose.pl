use strict;
require 'config.pl';
my $dir = $ARGV[0];
my $rmOrNot = $ARGV[1]  ? $ARGV[1] : "no";
my @files = <$dir/*>; 

for my $file (@files) {
	process($file);
}

sub process {
	my $file = shift;
	my $in = openReader($file);

	my @processing;
	my %written = (); 

	while(my $line = <$in>) {
		chomp $line;
		if ($line =~ m/^File opened (\S+)$/) {
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
			print "In $file\nNot closed: $p\n";
			call($cmd) if ($rmOrNot eq "rm");
		}
	}
}
