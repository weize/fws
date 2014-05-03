use strict;

my @files = </mnt/nfs/work3/wkong/tmp/stderr/galago-process-*>;

for my $file(@files) {
	print "cleaning for $file\n";
	open my $in, $file or die "cannot open $file";
	my @lines = <$in>;
	# looking for last processed file	
	my $last;
	my $docName = "";
	for ($last = $#lines; $last >= 0; $last --) {
		if ($lines[$last] =~m/^processing\s+(clueweb09-\S+)\n$/) {
			$docName = $1;
			last;
		}
	}
	die "cannot process $file" if ($last < 0);
	
	print "last processing $docName\n";
	my $processed = 0;
	for (; $last <= $#lines; $last ++) {
		if ($lines[$last] =~m/^(Written in .*|Warning: file exists\n)$/) {
			$processed = 1;	
			last;
		} 
	}

	if ($processed) {
		print "$docName has been processed\n";
	} else {
		print "Delete for $docName\n";
		#clueweb09-en0000-00-00688 	
		my @elems = split /-/, $docName;
		my @all = ();
		push @all, "../data/parse";
		push @all, @elems[1..2]; 
		push @all, "$docName.parse.gz"; 
		my $path = join("/", @all);
		print "rm $path\n";
		system("rm $path");
	}
	close $in;
}
