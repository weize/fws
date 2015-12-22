use strict;
my $path = $ARGV[0];
print "$path\n";
my @files = <$path>;

my %opens;
for my $file (@files) {
	open my $in, $file or die "Cannot open $file";
	while(my $line = <$in>) {
		chomp $line;
		if ($line =~ /^File opened (.+)$/) {
			my $open = $1; 
			$opens{$open} = 1;
		}
		if ($line =~ /^Writte in (.+)$/) {
			my $open = $1; 
			$opens{$open} = 0; 
		}
	}
	close $in;
}


for my $key (keys %opens) {
	if ($opens{$key} == 1) {
		print "$key\n";
	}
}
