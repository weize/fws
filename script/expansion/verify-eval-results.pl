use strict;
require 'config.pl';
my $expansionRunDir = "../exp/expansion/eval";

my @files = <$expansionRunDir/*/*.teval>;
for my $file (@files) {
	my $flag = check($file);
	if ($flag == 1) {
		print "\nerror in $file\n";
	} elsif ($flag == 2){
		print "\nnot 16 lines in $file\n";
	}
}

sub check {
	my $file = shift;
	my $in = openReader($file);
	my $ln = 0;
	while(my $line = <$in>) {
		$ln ++;
		chomp $line;
		if ($line =~ m/\S+\s+\S+\s+\d\.\d\d\d\d$/) {
		} else {
			return 1;
		}
	}
	close $in;
	if ($ln != 16) {
		return 2;
	}
	return 0;
}
