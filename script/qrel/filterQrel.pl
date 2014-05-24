# filter qrel so that all documents should be in $docsetFile
# and queries have at least one rel file.
use strict;
require 'config.pl';

our $config;

my $orgQrelFile = "../data/qrel/all.diversity.qrel";

#my $newQrelFile = "../data/qrel/all.diversity.filter.qrel";
#my $docsetFile = "../data/doc-name/clueweb09.docname";

my $newQrelFile = "../data/qrel/all.diversity.filter.spam60.qrel";
my $docsetFile = "../data/doc-name/clueweb09-spam60.docname";


my %docs; # docid (in origial qrel) -> boolean: in docset
my %qidRelDocs; # qid -> [doc1, doc2] (relevant doc list)

# load doc ids in qrels
my $reader = openReader($orgQrelFile);
while(my $line = <$reader>) {
	chomp $line;
	my ($qid, $Q0, $docid, $rating) = split /\s+/, $line;
	$docs{$docid} = 0;
	if ($rating > 0) {
		push(@{$qidRelDocs{$qid}}, $docid);
	}
}
close $reader;

# go through doc set to set %docs in the set
$reader = openReader($docsetFile);
while(my $docid = <$reader>) {
	chomp $docid;
	$docs{$docid} = 1 if (exists $docs{$docid});
}
close $reader;

my %qidsSelected; # queries that has at least one rel doc
for my $qid (keys %qidRelDocs) {
	my $hasRel = 0;
	for my $docid (@{$qidRelDocs{$qid}}) {
		if ($docs{$docid} == 1) {
			$hasRel = 1;
			last;
		}
	}
	if ($hasRel == 1) {
		$qidsSelected{$qid} = 1;
	}
}

$reader = openReader($orgQrelFile);
my $writer = openWriter($newQrelFile);
while(my $line = <$reader>) {
	chomp $line;
	my ($qid, $Q0, $docid, $rating) = split /\s+/, $line;
	if (exists $qidsSelected{$qid} and $docs{$docid} == 1) {
		print $writer "$line\n";
	}
}
close $reader;
close $writer;
infoWritten($newQrelFile);







