/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.db;

import javax.servlet.annotation.WebListener;


@WebListener
public class Indexes {

	public Indexes() {


	}
}

/*
var chuncks = db.getCollection('BlobChunks');
var files = db.getCollection('BlobFiles');
files.find({mobot:{$ne:null}}).forEach(function(img) {
    chuncks.deleteMany({f:img._id});
    files.deleteOne({_id:img._id});
    });
db.repairDatabase();

 */