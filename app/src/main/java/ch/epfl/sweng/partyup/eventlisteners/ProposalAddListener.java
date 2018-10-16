package ch.epfl.sweng.partyup.eventlisteners;

import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;

public interface ProposalAddListener {
    void addProposal(ProposalSchema newProposal);
}
