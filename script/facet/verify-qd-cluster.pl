use strict;
my $path = $ARGV[0];
print "$path\n";
my @files = </home2/wkong/work/tmp/stderr/galago-process-*>;

my %opens;
my $openCount = 0;
my $closeCount = 0;
for my $file (@files) {
	open my $in, $file or die "Cannot open $file";
	while(my $line = <$in>) {
		chomp $line;
		if ($line =~ /^File opened (.+)$/) {
			my $open = $1; 
			$opens{$open} = 1;
			$openCount ++;
		}
		if ($line =~ /^Writte in (.+)$/) {
			my $open = $1; 
			$opens{$open} = 0; 
			$closeCount ++;
		}
	}
	close $in;
}


for my $key (keys %opens) {
	if ($opens{$key} == 1) {
		print "$key\n";
	}
}
print "$openCount\t$closeCount\n";
