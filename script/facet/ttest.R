loadData <- function(run, model) {
	param <- if (model== "gmi") sprintf("sum-%s", metric) else "sum"
	file <- sprintf("facet-tune-gm-%s/%s/eval/%s.%s.10.eval", run, model, model, param);
	print(file)
	data <- read.csv(file, header=F, sep="\t")
	data <- data[data$V1!="all",] 
	data
}

args <- commandArgs(T)
run1 <- args[1];
model1 <- args[2];
run2 <- args[3];
model2 <- args[4];
metric <- strtoi(args[5]);
cat(sprintf("run=%s\tmodel=%s\n", run1, model1))
cat(sprintf("run=%s\tmodel=%s\n", run2, model2))
cat(sprintf("metric=%s\n", metric))

data1 <- loadData(run1, model1)
data2 <- loadData(run2, model2)


metric <- metric + 2
data1 <-data1[, metric];
data2 <-data2[, metric];
mean1 <- mean(data1);
mean2 <- mean(data2);
res <- t.test(data1,data2,paired=T)
diff <- mean1 - mean2
mean1
mean2
res

cat(sprintf("diff:%f\tp-value:%g\n", diff, res$p.value));
cat(sprintf("%g\n", res$p.value), file = stderr());
