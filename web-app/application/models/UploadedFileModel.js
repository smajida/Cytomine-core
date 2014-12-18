var UploadedFileModel = Backbone.Model.extend({

    initialize: function (options) {
        if (!options) {
            return;
        }
        this.image = options.image;
    },
    url: function () {
        var base = 'api/uploadedfile';
        var format = '.json';
        if(this.image != null && this.image != undefined) {
            return base + '/image/'+ this.image + format;
        } else{
            if (this.isNew()) {
                return base + format;
            }
            return base + (base.charAt(base.length - 1) == '/' ? '' : '/') + this.id + format;
        }
    }
});


var UploadedFileCollection = PaginatedCollection.extend({
    model: UploadedFileModel,
    initialize: function (options) {
        this.initPaginator(options);
        if (!options) {
            return;
        }
        this.dataTables = options.dataTables;
    },
    url: function () {
        if (this.dataTables) {
            return 'api/uploadedfile.json?dataTables=true';
        } else {
            return 'api/uploadedfile.json';
        }
    }
});