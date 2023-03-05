<?php
// http://URL/setCurrentTimeTemp.php/?time=10906788&temp=591
// http://URL/setCurrentTimeTemp.php?version

$version = "2.9";
$dbMinTime = 0;
if (isset($_GET["version"])) {
	echo nl2br("version=".$version.PHP_EOL);
} else {
	$fileName = "curTimeTemp.txt";
	$timeParam = $_GET["time"];
	$tempParam = $_GET["temp"];
	
	foreach(file($fileName) as $line) {
		$tmpArray = explode('=', rtrim($line));
		if ($tmpArray[0] == "minDBTime")
			$dbMinTime = $tmpArray[1];
	}

	$myfile = fopen($fileName, "w") or die("Unable to open file!");
	fwrite($myfile, "time=".$timeParam."\n");
	fwrite($myfile, "temp=".$tempParam."\n");
	fwrite($myfile, "minDBTime=".$dbMinTime."\n");
	fclose($myfile);
}
?>