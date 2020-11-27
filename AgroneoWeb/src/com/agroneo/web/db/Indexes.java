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

do {
var ids = [];
    files.find({mobot:{$ne:null}}).limit(200).forEach(function(img) {
        ids.push(img._id);
    });

    chuncks.deleteMany({f:{$in:ids}});
    files.deleteMany({_id:{$in:ids}});
} while (ids.length>0);

db.repairDatabase();

 */