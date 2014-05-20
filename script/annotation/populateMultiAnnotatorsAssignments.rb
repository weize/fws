#!/usr/bin/ruby

$:.unshift File.expand_path('.')
require 'annotation-backend.rb'

usage = "
Usage:  ruby populateAnnotatorAssignments.rb <file>

Where <file> lines of the following format
<annotatorID> \\t aolUserID1 \\t ...

"

inputFile = "../exp/annotation/assignment" 

db = AnnotationDB.new()

IO.foreach(inputFile) do |line|
    fields = line.chomp.split(/\t/)
    annotatorID = fields.shift
    aolUsers = fields
    print annotatorID, "\t", aolUsers, "\n"
    db.addAssignment( annotatorID, aolUsers )
end
