#!/usr/bin/perl
use strict;
use LWP::Simple;

# ------------------ MITAB FUNCTIONS ------------------

sub getXrefByDbName {
    my ( $xrefs, $dbName ) = @_;
    for my $xref (split (/\|/, $xrefs)){
        my ($db, $id, $txt) = split /[:\(\)]/, $xref;
         if( $db eq $dbName ) {
            return $id;
         }
    }

    return $xrefs;
}

# -----------------------------------------------------
# Note that we are only going to get 10 interactions at most
my $queryUrl = "http://www.ebi.ac.uk/Tools/webservices/psicquic/intact/webservices/current/search/query/BBC1?firstResult=0&maxResults=10";

my $content = get $queryUrl;
die "Couldn't get $queryUrl" unless defined $content;

#print $content;

# Now list all interactions
my @lines = split(/\n/, $content);
for my $line (@lines) {
  my @flds = split(/\t/, $line); # split tab delimited lines

  # split fields of a PSIMITAB 2.5 line
  my ($idA, $idB, $altIdA, $altIdB, $aliasA, $aliasB, $detMethod, $author, $pub, $orgA, $orgB, $intType, $sourceDb, $intId, $conf) = @flds;

  print getXrefByDbName($idA, "uniprotkb") . " interacts with " . getXrefByDbName($idB, "uniprotkb") . "\n";
}