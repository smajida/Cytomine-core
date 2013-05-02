var DashboardReviewPanel = Backbone.View.extend({
    width: null,
    software: null,
    project: null,
    parent: null,
    params: [],
    paramsViews: [],
    user : null,
    term: null,
    disableEvent: false,
    initialize: function (options) {
        this.el = "#tabs-review-" + this.model.id;
    },
    render: function (image,user,term) {
        var self = this;
        require([
            "text!application/templates/review/ReviewPanel.tpl.html"
        ],
            function (ReviewPanelTpl) {

                new ImageInstanceModel({id: image}).fetch({
                    success: function (model, response) {
                        $(self.el).empty();

                        var reviewer = null;
                        if(model.get('reviewUser')) {
                            reviewer = window.app.models.projectUser.get(model.get('reviewUser')).prettyName()
                        }

                        $(self.el).append(_.template(ReviewPanelTpl,{idImage: image,imageFilename:model.getVisibleName(window.app.status.currentProjectModel.get('blindMode')),projectName:self.model.get('name'),reviewer:reviewer}));

                        if(reviewer==null) {
                            $("a#startToReview"+image).click(function(evt){
                                 evt.preventDefault();

                                new ImageReviewModel({id: image}).save({}, {
                                    success: function (model, response) {
                                        window.app.view.message("Image", response.message, "success");
                                        window.location.reload();
                                    },
                                    error: function (model, response) {
                                        var json = $.parseJSON(response.responseText);
                                        window.app.view.message("Image", json.errors, "error");
                                     }});
                            });
                        }


                        self.initFilter(user,term);

                        self.initLastReview();

                        self.initAnnotationListing(image,user,term);

                        self.initTermListing();
                }});





            });
        return this;
    },
    refresh: function(image,user,term) {
        var self = this;
        self.annotationListing.render(self.model.id,image,user,term)
        self.changeSelectedFilter(user,term);
    },
    initLastReview : function() {
        console.log("*********************"+this.model.id);
        var self = this;
        self.lastReviewListing = new ReviewLastReviewListing({
            user : window.app.status.user.id,
            project : self.model.id,
            el: $("#lastReviewListing")
        }).render();
    },
    initAnnotationListing: function(image,user,term) {
        console.log("initAnnotationListing");
        var self = this;
        self.image = image;
        self.user = user;
        self.term = term;
        self.annotationListing = new ReviewAnnotationListing({
            page: undefined,
            model: null,
            term: undefined,
            el: $("#annotationReviewListing")
        }).render(self.model.id,image,user,term);


        $("#annotationReviewListing").find(".thumb-wrap").draggable( {
              containment: self.el,
              stack: "#annotationReviewListing .thumb-wrap",
              cursor: 'move',
              revert: true
            } );


    },
    initTermListing: function() {
        console.log("initTermListing");
        var self = this;

        var collection = window.app.status.currentTermsCollection;

        self.termListing= new ReviewTermListing({
            model: collection,
            el: $("#termReviewChoice")
        }).render();

        $("#termReviewChoice").find("div").droppable( {
            accept: '#annotationReviewListing .thumb-wrap',
            hoverClass: 'hovered',
            drop: self.handleCardDrop
          } );


    },



 handleCardDrop: function( event, ui ) {
     var self = this;
     console.log("handleCardDrop");

     console.log($(this));
     var slotTerm = $(this).attr("id");
     console.log("slotTerm="+slotTerm);
     var idTerm = slotTerm.replace("termDrop","");

     console.log(ui.draggable);
     console.log(ui.draggable.find("div"));
     console.log("ui.draggable.attr="+ui.draggable.find("div").attr("id"));
     var idThumb = ui.draggable.find("div").attr("id");
     var idAnnotation=idThumb.replace("mainAnnotationThumb-","");


     console.log("IdTerm="+idTerm);
     console.log("idAnnotation="+idAnnotation);

//     // If the card was dropped to the correct slot,
//     // change the card colour, position it directly
//     // on top of the slot, and prevent it being dragged
//     // again
//
//     if ( slotNumber == cardNumber ) {
//       ui.draggable.addClass( 'correct' );
//       ui.draggable.draggable( 'disable' );
//       $(this).droppable( 'disable' );
//       ui.draggable.position( { of: $(this), my: 'left top', at: 'left top' } );
//       ui.draggable.draggable( 'option', 'revert', false );
//       self.correctCards++;
//     }

   },
    initFilter : function(user, term) {
        var self = this;
        var userFilter = $("#filterReviewListing").find("#filterUser");
        userFilter.empty();
        userFilter.append('<option value="-1">All</option>');

        window.app.models.projectUser.each(function(user) {
            userFilter.append('<option value="'+user.id+'">'+user.prettyName()+'</option>');
        });
        window.app.models.projectUserJob.each(function(user) {
            userFilter.append('<option value="'+user.id+'">'+user.prettyName()+'</option>');
        });

        var termFilter = $("#filterReviewListing").find("#filterTerm");
        termFilter.empty();
        termFilter.append('<option value="-1">All</option>');
        window.app.status.currentTermsCollection.each(function(term) {
            termFilter.append('<option value="'+term.id+'">'+term.get('name')+'</option>');
        });

        var changeFilter = function() {
            var userValue = userFilter.val();
            var termValue = termFilter.val();
            if(userValue==-1) {
                userValue = null;
            }
            if(termValue==-1) {
                termValue = null;
            }
            if(!self.disableEvent) {
               window.location = "#tabs-review-"+self.model.id+"-"+self.image+"-"+ userValue + "-" + termValue
            }
        }


        userFilter.change(changeFilter);
        termFilter.change(changeFilter);


        self.changeSelectedFilter(user,term)

    },
    changeSelectedFilter : function (user, term) {
        var self = this;
        var userFilter = $("#filterReviewListing").find("#filterUser");
        var termFilter = $("#filterReviewListing").find("#filterTerm");

        self.disableEvent = true;
        if(user) {
            userFilter.val(user);
        } else {
            userFilter.val(-1);
        }

        if(term) {
            termFilter.val(term);
        } else {
            termFilter.val(-1);
        }
        self.disableEvent = false;
    }

});