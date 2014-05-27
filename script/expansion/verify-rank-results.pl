use strict;
require 'config.pl';
my $expansionRunDir = "../exp/expansion/run";

my @files = <$expansionRunDir/*/*.rank>;
for my $file (@files) {
	my $flag = check($file);
	if ($flag == 1) {
		print "error in $file\n";
	} elsif ($flag == 2){
		print "not 1000 lines in $file\n";
	}
}

sub check {
	my $file = shift;
	my $in = openReader($file);
	my $ln = 0;
	while(my $line = <$in>) {
		$ln ++;
		chomp $line;
		if ($line =~ m/.+ galago$/) {
		} else {
			return 1;
		}
	}
	close $in;
	if ($ln != 1000) {
		return 2;
	}
	return 0;
}
